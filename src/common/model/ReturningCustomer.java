package common.model;

/**
 * A customer who has already completed at least one purchase, but has not yet
 * reached the VIP threshold.
 * <p>
 * <b>Purchase plan:</b> a five percent loyalty discount on every purchase, which
 * grows to eight percent when the customer buys three items or more in the same
 * sale. This plan rewards quantity, which is the difference between it and the
 * {@link VipCustomer} plan that rewards the total amount of money.
 * </p>
 */
public class ReturningCustomer extends Customer {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The loyalty discount granted on every purchase: five percent. */
    private static final double LOYALTY_DISCOUNT_RATE = 0.05;

    /** The improved discount granted on a bulk purchase: eight percent. */
    private static final double BULK_DISCOUNT_RATE = 0.08;

    /** The number of items from which a purchase is considered a bulk purchase. */
    private static final int BULK_MINIMUM_QUANTITY = 3;

    /**
     * Creates a returning customer with an empty purchase history.
     *
     * @param idNumber the national identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     */
    public ReturningCustomer(String idNumber, String fullName, String phone) {
        super(idNumber, fullName, phone);
    }

    /**
     * Creates a returning customer with an existing purchase history. Used by
     * {@link CustomerFactory} when copying data between customer kinds.
     *
     * @param idNumber      the national identity number, the unique key of a customer
     * @param fullName      the full name of the customer
     * @param phone         the phone number of the customer
     * @param purchaseCount how many purchases the customer has completed
     * @param totalSpent    how much money the customer has paid in total
     */
    public ReturningCustomer(String idNumber, String fullName, String phone,
                             int purchaseCount, double totalSpent) {
        super(idNumber, fullName, phone, purchaseCount, totalSpent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses the bulk rate when the quantity reaches
     * {@value #BULK_MINIMUM_QUANTITY} items, and the regular loyalty rate
     * otherwise.
     * </p>
     */
    @Override
    public double calculateFinalPrice(double unitPrice, int quantity) {
        double totalBeforeDiscount = unitPrice * quantity;
        if (quantity >= BULK_MINIMUM_QUANTITY) {
            return applyDiscount(totalBeforeDiscount, BULK_DISCOUNT_RATE);
        }
        return applyDiscount(totalBeforeDiscount, LOYALTY_DISCOUNT_RATE);
    }

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.RETURNING;
    }

    @Override
    public String getPurchasePlanDescription() {
        return "Returning customer: 5% loyalty discount, 8% when buying "
                + BULK_MINIMUM_QUANTITY + " items or more";
    }
}
