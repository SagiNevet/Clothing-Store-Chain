package common.model;

public class ReturningCustomer extends Customer {

    private static final long serialVersionUID = 1L;

    private static final double LOYALTY_DISCOUNT_RATE = 0.05;

    private static final double BULK_DISCOUNT_RATE = 0.08;

    private static final int BULK_MINIMUM_QUANTITY = 3;

    public ReturningCustomer(String idNumber, String fullName, String phone) {
        super(idNumber, fullName, phone);
    }

    public ReturningCustomer(String idNumber, String fullName, String phone,
                             int purchaseCount, double totalSpent) {
        super(idNumber, fullName, phone, purchaseCount, totalSpent);
    }

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
