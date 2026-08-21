package common.model;

/**
 * A customer who has not completed a purchase yet.
 * <p>
 * <b>Purchase plan:</b> a ten percent welcome discount on the very first
 * purchase, and the full catalogue price afterwards. In practice the second
 * case almost never happens, because a customer is moved to
 * {@link ReturningCustomer} immediately after the first purchase - but the
 * subclass still has to answer correctly for any input it is given.
 * </p>
 */
public class NewCustomer extends Customer {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The welcome discount granted on the first purchase: ten percent. */
    private static final double WELCOME_DISCOUNT_RATE = 0.10;

    /** A purchase history of this size means the customer has never bought anything. */
    private static final int NO_PURCHASES_YET = 0;

    /**
     * Creates a new customer with an empty purchase history.
     *
     * @param idNumber the national identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     */
    public NewCustomer(String idNumber, String fullName, String phone) {
        super(idNumber, fullName, phone);
    }

    /**
     * Creates a new customer with an existing purchase history. Used by
     * {@link CustomerFactory} when copying data between customer kinds.
     *
     * @param idNumber      the national identity number, the unique key of a customer
     * @param fullName      the full name of the customer
     * @param phone         the phone number of the customer
     * @param purchaseCount how many purchases the customer has completed
     * @param totalSpent    how much money the customer has paid in total
     */
    public NewCustomer(String idNumber, String fullName, String phone,
                       int purchaseCount, double totalSpent) {
        super(idNumber, fullName, phone, purchaseCount, totalSpent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Grants the welcome discount only while the purchase history is still
     * empty. The {@code if} here asks about the history of this customer, not
     * about the kind of customer, so the polymorphic design is preserved.
     * </p>
     */
    @Override
    public double calculateFinalPrice(double unitPrice, int quantity) {
        double totalBeforeDiscount = unitPrice * quantity;
        if (getPurchaseCount() == NO_PURCHASES_YET) {
            return applyDiscount(totalBeforeDiscount, WELCOME_DISCOUNT_RATE);
        }
        return applyDiscount(totalBeforeDiscount, NO_DISCOUNT_RATE);
    }

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.NEW;
    }

    @Override
    public String getPurchasePlanDescription() {
        return "New customer: 10% welcome discount on the first purchase";
    }
}
