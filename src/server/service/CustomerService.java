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

public class CustomerService {

    private final Map<String, Customer> customersById = new LinkedHashMap<>();

    private final ConcurrentHashMap<String, Object> customerLocks = new ConcurrentHashMap<>();

    public CustomerService() throws StorageException {
        for (Customer customer : repository().loadAll()) {
            customersById.put(customer.getIdNumber(), customer);
        }
    }

    public List<Customer> getAllCustomers() {
        synchronized (customersById) {
            return new ArrayList<>(customersById.values());
        }
    }

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

    public Customer addCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Customer newCustomer = CustomerFactory.createNewCustomer(idNumber, fullName, phone);

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

    public Customer registerPurchase(String idNumber, double amountPaid)
            throws ChainStoreException {
        Customer customerAfterPurchase;

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

    private Object lockFor(String idNumber) {
        return customerLocks.computeIfAbsent(idNumber, key -> new Object());
    }

    private void persist() throws StorageException {
        List<Customer> customersToSave;
        synchronized (customersById) {
            customersToSave = new ArrayList<>(customersById.values());
        }
        repository().saveAll(customersToSave);
    }

    private CustomerRepository repository() {
        return ServerContext.getInstance().getCustomerRepository();
    }
}
