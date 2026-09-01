package common.model;

public class VipCustomer extends Customer {

    private static final long serialVersionUID = 1L;

    private static final double VIP_DISCOUNT_RATE = 0.15;

    private static final double LARGE_ORDER_DISCOUNT_RATE = 0.20;

    private static final double LARGE_ORDER_THRESHOLD = 500.0;

    public VipCustomer(String idNumber, String fullName, String phone) {
        super(idNumber, fullName, phone);
    }

    public VipCustomer(String idNumber, String fullName, String phone,
                       int purchaseCount, double totalSpent) {
        super(idNumber, fullName, phone, purchaseCount, totalSpent);
    }

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
