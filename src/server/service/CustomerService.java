package server.service;

import common.exception.ChainStoreException;
import common.exception.EntityNotFoundException;
import common.exception.StorageException;
import common.model.Customer;
import common.model.CustomerFactory;
import server.core.ServerContext;
import server.storage.CustomerRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the customer list of the whole chain and records their purchases.
 * <p>
 * Unlike the inventory, this list is <b>shared by every branch</b>: a customer
 * registered in Tel Aviv is immediately known in Jerusalem, and a change here is
 * pushed to every connected employee of the chain.
 * </p>
 * <p>
 * <b>Where the polymorphism of the project actually happens.</b> This service
 * never asks what kind of customer it holds. When a purchase is recorded it
 * calls {@link Customer#registerPurchase(double)} and then
 * {@link CustomerFactory#upgradeIfNeeded(Customer)}, and the factory returns
 * either the same object or a brand new object of the upgraded class. The price
 * itself is calculated by the caller with one polymorphic call to
 * {@link Customer#calculateFinalPrice(double, int)}.
 * </p>
 * <p>
 * <b>The locking follows the same principle as everywhere else in this
 * project:</b> one lock per customer rather than one lock for the whole
 * service. Recording a purchase is a read, then a change, then a write back -
 * and all three must happen without another thread slipping in between,
 * otherwise a customer who bought in both branches at the same instant would
 * lose one of the two purchases. Two <b>different</b> customers never wait for
 * one another.
 * </p>
 */
public class CustomerService {

    /** Every customer of the chain, keyed by identity number. */
    private final Map<String, Customer> customersById = new LinkedHashMap<>();

    /** One lock object per customer, so different customers never block each other. */
    private final ConcurrentHashMap<String, Object> customerLocks = new ConcurrentHashMap<>();

    /**
     * Loads the customer list from the file into memory.
     *
     * @throws StorageException if the customers file cannot be read
     */
    public CustomerService() throws StorageException {
        for (Customer customer : repository().loadAll()) {
            customersById.put(customer.getIdNumber(), customer);
        }
    }

    /**
     * Returns the whole customer list of the chain.
     *
     * @return a list holding every customer, in the order they were registered
     */
    public List<Customer> getAllCustomers() {
        synchronized (customersById) {
            return new ArrayList<>(customersById.values());
        }
    }

    /**
     * Finds one customer by identity number.
     *
     * @param idNumber the identity number to look for
     * @return the customer object held by the server
     * @throws EntityNotFoundException if no such customer is registered
     */
    public Customer findCustomer(String idNumber) throws EntityNotFoundException {
        Customer customer;
        synchronized (customersById) {
            customer = customersById.get(idNumber);
        }
        if (customer == null) {
            throw new EntityNotFoundException("Customer", idNumber);
        }
        return customer;
    }

    /**
     * Registers a brand new customer of the chain.
     * <p>
     * Every customer starts as a {@code NewCustomer}, which is the only kind
     * that grants the welcome discount.
     * </p>
     *
     * @param idNumber the identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     * @return the customer that was created
     * @throws ChainStoreException if that identity number is already registered,
     *                             or if the customers file cannot be written
     */
    public Customer addCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Customer newCustomer = CustomerFactory.createNewCustomer(idNumber, fullName, phone);

        // The check and the insertion are locked together, so the same identity
        // number cannot be registered twice from two branches at once.
        synchronized (customersById) {
            if (customersById.containsKey(idNumber)) {
                throw new ChainStoreException(
                        "A customer with the identity number " + idNumber + " is already registered");
            }
            customersById.put(idNumber, newCustomer);
        }

        persist();
        return newCustomer;
    }

    /**
     * Updates the name and the phone number of an existing customer.
     * <p>
     * The kind of customer is never changed by hand: it is decided only by the
     * purchase history, through {@link CustomerFactory}.
     * </p>
     *
     * @param idNumber the customer to update
     * @param fullName the new full name
     * @param phone    the new phone number
     * @return the updated customer
     * @throws ChainStoreException if the customer does not exist or the file
     *                             cannot be written
     */
    public Customer updateCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Customer customer = findCustomer(idNumber);

        synchronized (lockFor(idNumber)) {
            customer.setFullName(fullName);
            customer.setPhone(phone);
        }

        persist();
        return customer;
    }

    /**
     * Records a completed purchase and moves the customer to another kind when
     * the criteria are met.
     *
     * @param idNumber   the customer that bought
     * @param amountPaid the final price the customer actually paid
     * @return the customer after the purchase, which may be a <b>new object</b>
     *         of an upgraded kind
     * @throws ChainStoreException if the customer does not exist or the file
     *                             cannot be written
     */
    public Customer registerPurchase(String idNumber, double amountPaid)
            throws ChainStoreException {
        Customer customerAfterPurchase;

        // Read, change and write back, all inside one lock on this one customer.
        // The customer is read again inside the lock on purpose: reading it
        // before entering would allow another thread to replace it in the map
        // between the read and the change.
        synchronized (lockFor(idNumber)) {
            Customer currentCustomer = findCustomer(idNumber);
            currentCustomer.registerPurchase(amountPaid);
            customerAfterPurchase = CustomerFactory.upgradeIfNeeded(currentCustomer);
            synchronized (customersById) {
                customersById.put(idNumber, customerAfterPurchase);
            }
        }

        persist();
        return customerAfterPurchase;
    }

    /**
     * Returns the lock object of one customer, creating it the first time.
     * <p>
     * {@link ConcurrentHashMap#computeIfAbsent} guarantees that two threads
     * asking for the lock of the same customer at the same moment receive the
     * very same object - which is the whole point of a lock.
     * </p>
     *
     * @param idNumber the customer whose lock is needed
     * @return the lock object of that customer
     */
    private Object lockFor(String idNumber) {
        return customerLocks.computeIfAbsent(idNumber, key -> new Object());
    }

    /**
     * Writes the whole customer list back to its file.
     *
     * @throws StorageException if the customers file cannot be written
     */
    private void persist() throws StorageException {
        List<Customer> customersToSave;
        synchronized (customersById) {
            customersToSave = new ArrayList<>(customersById.values());
        }
        repository().saveAll(customersToSave);
    }

    /**
     * Returns the shared repository of the customer list.
     *
     * @return the customer repository
     */
    private CustomerRepository repository() {
        return ServerContext.getInstance().getCustomerRepository();
    }
}
