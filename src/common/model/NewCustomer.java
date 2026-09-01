package common.model;

public class NewCustomer extends Customer {

    private static final long serialVersionUID = 1L;

    private static final double WELCOME_DISCOUNT_RATE = 0.10;

    private static final int NO_PURCHASES_YET = 0;

    public NewCustomer(String idNumber, String fullName, String phone) {
        super(idNumber, fullName, phone);
    }

    public NewCustomer(String idNumber, String fullName, String phone,
                       int purchaseCount, double totalSpent) {
        super(idNumber, fullName, phone, purchaseCount, totalSpent);
    }

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
