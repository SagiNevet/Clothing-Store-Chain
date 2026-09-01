package common.model;

import java.io.Serializable;

public abstract class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final double NO_DISCOUNT_RATE = 0.0;

    private static final double MONEY_ROUNDING_FACTOR = 100.0;

    private final String idNumber;

    private String fullName;

    private String phone;

    private int purchaseCount;

    private double totalSpent;

    protected Customer(String idNumber, String fullName, String phone) {
        this(idNumber, fullName, phone, 0, 0.0);
    }

    protected Customer(String idNumber, String fullName, String phone,
                       int purchaseCount, double totalSpent) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.purchaseCount = purchaseCount;
        this.totalSpent = totalSpent;
    }

    public abstract double calculateFinalPrice(double unitPrice, int quantity);

    public abstract CustomerType getCustomerType();

    public abstract String getPurchasePlanDescription();

    protected static double applyDiscount(double amount, double discountRate) {
        double amountAfterDiscount = amount * (1.0 - discountRate);
        return Math.round(amountAfterDiscount * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
    }

    public void registerPurchase(double amountPaid) {
        this.purchaseCount++;
        this.totalSpent += amountPaid;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer)) {
            return false;
        }
        Customer otherCustomer = (Customer) other;
        return idNumber.equals(otherCustomer.idNumber);
    }

    @Override
    public int hashCode() {
        return idNumber.hashCode();
    }

    @Override
    public String toString() {
        return getCustomerType().getDisplayName() + " " + fullName
                + " (id " + idNumber + ", phone " + phone
                + ", purchases " + purchaseCount + ", total " + totalSpent + ")";
    }
}
