package common.model;

import common.util.AppConfig;

/**
 * Creates customer objects and moves a customer from one kind to another.
 * <p>
 * This class is the <b>Factory</b> pattern of the customer hierarchy. It is the
 * only place in the whole system that mentions the three concrete classes
 * {@link NewCustomer}, {@link ReturningCustomer} and {@link VipCustomer} by
 * name. Everywhere else the code works with the abstract {@link Customer} type,
 * which is what keeps the sale logic free of any check on the customer kind.
 * </p>
 * <p>
 * <b>Why a customer object is replaced and not modified:</b> the kind of a
 * customer is expressed by the class of the object, because that is what makes
 * the polymorphic call work. Java cannot change the class of an existing
 * object, so an upgrade creates a new object of the target class and copies the
 * purchase history into it. The identity number stays the same, and because
 * {@link Customer#equals(Object)} compares identity numbers only, the new
 * object replaces the old one correctly inside every collection.
 * </p>
 * <p>
 * <b>The upgrade criteria, as documented in the README:</b>
 * </p>
 * <ul>
 *   <li>{@link CustomerType#NEW} becomes {@link CustomerType#RETURNING} after
 *       the first completed purchase.</li>
 *   <li>{@link CustomerType#RETURNING} becomes {@link CustomerType#VIP} after
 *       {@value #DEFAULT_VIP_MINIMUM_PURCHASES} purchases <b>or</b> once the
 *       accumulated spending passes
 *       {@value #DEFAULT_VIP_MINIMUM_TOTAL_SPENT}.</li>
 * </ul>
 */
public final class CustomerFactory {

    /** Configuration key holding the number of purchases required for VIP. */
    private static final String CONFIG_KEY_VIP_MINIMUM_PURCHASES = "customer.vip.minPurchases";

    /** Configuration key holding the accumulated spending required for VIP. */
    private static final String CONFIG_KEY_VIP_MINIMUM_TOTAL_SPENT = "customer.vip.minTotalSpent";

    /** Number of purchases required for VIP when the configuration file is missing. */
    private static final int DEFAULT_VIP_MINIMUM_PURCHASES = 5;

    /** Accumulated spending required for VIP when the configuration file is missing. */
    private static final double DEFAULT_VIP_MINIMUM_TOTAL_SPENT = 1000.0;

    /** A purchase history of this size means the customer has never bought anything. */
    private static final int NO_PURCHASES_YET = 0;

    /**
     * Prevents instantiation. This class only exposes static factory methods.
     */
    private CustomerFactory() {
    }

    /**
     * Creates a customer of the requested kind with an empty purchase history.
     *
     * @param customerType the kind of customer to create
     * @param idNumber     the national identity number, the unique key of a customer
     * @param fullName     the full name of the customer
     * @param phone        the phone number of the customer
     * @return a new customer object of the matching concrete class
     * @throws IllegalArgumentException if {@code customerType} is {@code null}
     */
    public static Customer create(CustomerType customerType, String idNumber,
                                  String fullName, String phone) {
        return create(customerType, idNumber, fullName, phone, NO_PURCHASES_YET, 0.0);
    }

    /**
     * Creates a customer of the requested kind carrying an existing purchase
     * history.
     *
     * @param customerType  the kind of customer to create
     * @param idNumber      the national identity number, the unique key of a customer
     * @param fullName      the full name of the customer
     * @param phone         the phone number of the customer
     * @param purchaseCount how many purchases the customer has completed
     * @param totalSpent    how much money the customer has paid in total
     * @return a new customer object of the matching concrete class
     * @throws IllegalArgumentException if {@code customerType} is {@code null}
     */
    public static Customer create(CustomerType customerType, String idNumber, String fullName,
                                  String phone, int purchaseCount, double totalSpent) {
        if (customerType == null) {
            throw new IllegalArgumentException("customerType must not be null");
        }
        switch (customerType) {
            case NEW:
                return new NewCustomer(idNumber, fullName, phone, purchaseCount, totalSpent);
            case RETURNING:
                return new ReturningCustomer(idNumber, fullName, phone, purchaseCount, totalSpent);
            case VIP:
                return new VipCustomer(idNumber, fullName, phone, purchaseCount, totalSpent);
            default:
                throw new IllegalArgumentException("Unsupported customer type: " + customerType);
        }
    }

    /**
     * Registers a brand new customer of the chain.
     *
     * @param idNumber the national identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     * @return a {@link NewCustomer} with an empty purchase history
     */
    public static Customer createNewCustomer(String idNumber, String fullName, String phone) {
        return new NewCustomer(idNumber, fullName, phone);
    }

    /**
     * Returns the customer object that matches the current purchase history.
     * <p>
     * Called by the sale logic right after
     * {@link Customer#registerPurchase(double)}. When the customer already
     * belongs to the correct kind, the very same object is returned and nothing
     * is copied.
     * </p>
     *
     * @param customer the customer to examine, must not be {@code null}
     * @return the same object when no change is needed, or a new object of the
     *         upgraded kind carrying the same data
     * @throws IllegalArgumentException if {@code customer} is {@code null}
     */
    public static Customer upgradeIfNeeded(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("customer must not be null");
        }
        CustomerType requiredType = determineTypeFor(customer);
        if (requiredType == customer.getCustomerType()) {
            return customer;
        }
        return create(requiredType,
                customer.getIdNumber(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPurchaseCount(),
                customer.getTotalSpent());
    }

    /**
     * Decides which kind of customer matches a purchase history.
     *
     * @param customer the customer to examine
     * @return the customer kind the history entitles the customer to
     */
    private static CustomerType determineTypeFor(Customer customer) {
        if (customer.getPurchaseCount() == NO_PURCHASES_YET) {
            return CustomerType.NEW;
        }
        if (qualifiesForVip(customer)) {
            return CustomerType.VIP;
        }
        return CustomerType.RETURNING;
    }

    /**
     * Checks the VIP criteria against a purchase history.
     *
     * @param customer the customer to examine
     * @return {@code true} if the customer reached the purchase count threshold
     *         or the accumulated spending threshold
     */
    private static boolean qualifiesForVip(Customer customer) {
        AppConfig configuration = AppConfig.getInstance();
        int minimumPurchases = configuration.getInt(
                CONFIG_KEY_VIP_MINIMUM_PURCHASES, DEFAULT_VIP_MINIMUM_PURCHASES);
        double minimumTotalSpent = configuration.getDouble(
                CONFIG_KEY_VIP_MINIMUM_TOTAL_SPENT, DEFAULT_VIP_MINIMUM_TOTAL_SPENT);
        return customer.getPurchaseCount() >= minimumPurchases
                || customer.getTotalSpent() >= minimumTotalSpent;
    }
}
