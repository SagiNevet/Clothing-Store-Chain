package common.model;

import java.io.Serializable;

/**
 * A customer of the chain, and the base of the customer type hierarchy.
 * <p>
 * <b>This class is the centre of the polymorphism requirement.</b> Every kind of
 * customer has a different purchase plan, and each plan lives inside its own
 * subclass: {@link NewCustomer}, {@link ReturningCustomer} and
 * {@link VipCustomer}. The selling code calls
 * {@link #calculateFinalPrice(double, int)} once and never asks what kind of
 * customer it holds - there is no {@code if} on the customer type anywhere in
 * the calling code. Adding a fourth kind of customer in the future means adding
 * one new subclass, without touching the sale logic at all.
 * </p>
 * <p>
 * The customer list is shared by the whole chain and is stored in a single file
 * on the server, therefore the class implements {@link Serializable}.
 * </p>
 */
public abstract class Customer implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** Discount rate meaning "no discount at all". */
    protected static final double NO_DISCOUNT_RATE = 0.0;

    /** The number of digits kept when rounding a money amount. */
    private static final double MONEY_ROUNDING_FACTOR = 100.0;

    /** The national identity number of the customer. Never changes, so it is final. */
    private final String idNumber;

    /** The full name of the customer. */
    private String fullName;

    /** The phone number of the customer. */
    private String phone;

    /** How many purchases this customer has completed so far. */
    private int purchaseCount;

    /** The total amount of money this customer has paid so far. */
    private double totalSpent;

    /**
     * Creates a customer with an empty purchase history.
     *
     * @param idNumber the national identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     */
    protected Customer(String idNumber, String fullName, String phone) {
        this(idNumber, fullName, phone, 0, 0.0);
    }

    /**
     * Creates a customer with an existing purchase history.
     * <p>
     * This constructor is used by {@link CustomerFactory} when a customer moves
     * from one kind to another: a brand new object of the target subclass is
     * created, and the history is copied into it so that nothing is lost.
     * </p>
     *
     * @param idNumber      the national identity number, the unique key of a customer
     * @param fullName      the full name of the customer
     * @param phone         the phone number of the customer
     * @param purchaseCount how many purchases the customer has completed
     * @param totalSpent    how much money the customer has paid in total
     */
    protected Customer(String idNumber, String fullName, String phone,
                       int purchaseCount, double totalSpent) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.purchaseCount = purchaseCount;
        this.totalSpent = totalSpent;
    }

    /**
     * Calculates the price this customer actually pays, according to the
     * purchase plan of the customer kind.
     * <p>
     * Every subclass implements this method differently. This single abstract
     * method is what allows the sale code to stay free of any check on the
     * customer type.
     * </p>
     *
     * @param unitPrice the catalogue price of one item, must not be negative
     * @param quantity  how many items are being bought, must be positive
     * @return the final total price to charge, rounded to two decimal places
     */
    public abstract double calculateFinalPrice(double unitPrice, int quantity);

    /**
     * Returns the label of this customer kind, used by the GUI and by the logs.
     *
     * @return the matching {@link CustomerType} constant, never {@code null}
     */
    public abstract CustomerType getCustomerType();

    /**
     * Returns a short sentence describing the purchase plan of this customer
     * kind, so the seller can explain the price to the customer.
     *
     * @return a human readable description of the purchase plan
     */
    public abstract String getPurchasePlanDescription();

    /**
     * Applies a discount rate to an amount of money and rounds the result to
     * two decimal places.
     * <p>
     * Rounding here, in one shared place, keeps every purchase plan consistent
     * and avoids prices such as 84.99999999999999 which come from the way
     * {@code double} represents fractions.
     * </p>
     *
     * @param amount       the amount before the discount
     * @param discountRate the discount as a fraction, for example {@code 0.15} for 15 percent
     * @return the amount after the discount, rounded to two decimal places
     */
    protected static double applyDiscount(double amount, double discountRate) {
        double amountAfterDiscount = amount * (1.0 - discountRate);
        return Math.round(amountAfterDiscount * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
    }

    /**
     * Records a completed purchase in the history of this customer.
     * <p>
     * The history is what decides whether the customer should move to another
     * kind, which is checked by
     * {@link CustomerFactory#upgradeIfNeeded(Customer)} right after this call.
     * </p>
     *
     * @param amountPaid the final price the customer paid in this purchase
     */
    public void registerPurchase(double amountPaid) {
        this.purchaseCount++;
        this.totalSpent += amountPaid;
    }

    /**
     * Returns the national identity number, which is the unique key of a customer.
     *
     * @return the identity number, never {@code null}
     */
    public String getIdNumber() {
        return idNumber;
    }

    /**
     * Returns the full name of the customer.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Updates the full name of the customer.
     *
     * @param fullName the new full name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the phone number of the customer.
     *
     * @return the phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Updates the phone number of the customer.
     *
     * @param phone the new phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns how many purchases this customer has completed.
     *
     * @return the number of completed purchases, never negative
     */
    public int getPurchaseCount() {
        return purchaseCount;
    }

    /**
     * Returns how much money this customer has paid in total.
     *
     * @return the accumulated amount, never negative
     */
    public double getTotalSpent() {
        return totalSpent;
    }

    /**
     * Compares customers by their identity number only.
     * <p>
     * Two objects describe the same person when the identity numbers match,
     * even if one of them is a {@link NewCustomer} and the other is already a
     * {@link VipCustomer}. That is exactly what happens during an upgrade, so
     * comparing the classes here would be wrong.
     * </p>
     *
     * @param other the object to compare with
     * @return {@code true} if both objects describe the same person
     */
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

    /**
     * Returns a hash code derived from the identity number.
     * <p>
     * {@code hashCode} is overridden together with {@code equals} because a
     * customer is stored inside hash based collections such as
     * {@code HashMap} and {@code HashSet}. Two objects that are equal must
     * return the same hash code, otherwise the collection would keep two
     * entries for the same person.
     * </p>
     *
     * @return the hash code of the identity number
     */
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
