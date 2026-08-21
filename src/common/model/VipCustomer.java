package common.model;

/**
 * A loyal customer who passed the VIP threshold defined by
 * {@link CustomerFactory}.
 * <p>
 * <b>Purchase plan:</b> a fifteen percent discount on every purchase, which
 * grows to twenty percent when the value of the sale passes
 * {@value #LARGE_ORDER_THRESHOLD}. This plan rewards the amount of money spent
 * in the sale, unlike {@link ReturningCustomer} which rewards the number of
 * items.
 * </p>
 */
public class VipCustomer extends Customer {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The regular VIP discount: fifteen percent. */
    private static final double VIP_DISCOUNT_RATE = 0.15;

    /** The improved discount granted on a large order: twenty percent. */
    private static final double LARGE_ORDER_DISCOUNT_RATE = 0.20;

    /** The order value, before any discount, from which the improved rate applies. */
    private static final double LARGE_ORDER_THRESHOLD = 500.0;

    /**
     * Creates a VIP customer with an empty purchase history.
     *
     * @param idNumber the national identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     */
    public VipCustomer(String idNumber, String fullName, String phone) {
        super(idNumber, fullName, phone);
    }

    /**
     * Creates a VIP customer with an existing purchase history. Used by
     * {@link CustomerFactory} when copying data between customer kinds.
     *
     * @param idNumber      the national identity number, the unique key of a customer
     * @param fullName      the full name of the customer
     * @param phone         the phone number of the customer
     * @param purchaseCount how many purchases the customer has completed
     * @param totalSpent    how much money the customer has paid in total
     */
    public VipCustomer(String idNumber, String fullName, String phone,
                       int purchaseCount, double totalSpent) {
        super(idNumber, fullName, phone, purchaseCount, totalSpent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The threshold is examined on the price <b>before</b> the discount, so the
     * discount itself can never push an order below the threshold and cancel
     * the very benefit that was just granted.
     * </p>
     */
    @Override
    public double calculateFinalPrice(double unitPrice, int quantity) {
        double totalBeforeDiscount = unitPrice * quantity;
        if (totalBeforeDiscount > LARGE_ORDER_THRESHOLD) {
            return applyDiscount(totalBeforeDiscount, LARGE_ORDER_DISCOUNT_RATE);
        }
        return applyDiscount(totalBeforeDiscount, VIP_DISCOUNT_RATE);
    }

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.VIP;
    }

    @Override
    public String getPurchasePlanDescription() {
        return "VIP customer: 15% discount, 20% on orders above "
                + LARGE_ORDER_THRESHOLD;
    }
}
