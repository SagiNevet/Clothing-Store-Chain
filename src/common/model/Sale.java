package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One completed sale, kept as the permanent record the reports are built from.
 * <p>
 * A sale copies the details of the product and of the customer into its own
 * fields instead of holding references to them. The reason is that a report
 * must describe what happened <b>at the moment of the sale</b>: if the price of
 * a shirt changes next month, last month sales must still show the old price.
 * </p>
 * <p>
 * The object stores both the price before the discount and the price actually
 * paid, so a report can show how much the purchase plan of the customer was
 * worth.
 * </p>
 */
public class Sale implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The unique identifier of this sale. */
    private final String saleId;

    /** The moment the sale was completed. */
    private final LocalDateTime saleTime;

    /** The branch the sale was made in. */
    private final Branch branch;

    /** The employee number of the seller. */
    private final String sellerEmployeeNumber;

    /** The identity number of the buying customer. */
    private final String customerIdNumber;

    /** The kind of customer at the moment of the sale, before any upgrade. */
    private final CustomerType customerTypeAtSale;

    /** The identifier of the product that was sold. */
    private final String productId;

    /** The name of the product at the moment of the sale. */
    private final String productName;

    /** The category of the product at the moment of the sale. */
    private final ProductCategory category;

    /** How many items were sold. */
    private final int quantity;

    /** The catalogue price of a single item at the moment of the sale. */
    private final double unitPrice;

    /** The total price before the discount of the purchase plan. */
    private final double totalBeforeDiscount;

    /** The total price the customer actually paid. */
    private final double finalPrice;

    /**
     * Creates a sale record.
     *
     * @param saleId               the unique identifier of the sale
     * @param branch               the branch the sale was made in
     * @param sellerEmployeeNumber the employee number of the seller
     * @param customerIdNumber     the identity number of the buying customer
     * @param customerTypeAtSale   the kind of customer at the moment of the sale
     * @param product              the product that was sold, its details are copied
     * @param quantity             how many items were sold
     * @param finalPrice           the total price the customer actually paid
     */
    public Sale(String saleId, Branch branch, String sellerEmployeeNumber,
                String customerIdNumber, CustomerType customerTypeAtSale,
                Product product, int quantity, double finalPrice) {
        this.saleId = saleId;
        this.saleTime = LocalDateTime.now();
        this.branch = branch;
        this.sellerEmployeeNumber = sellerEmployeeNumber;
        this.customerIdNumber = customerIdNumber;
        this.customerTypeAtSale = customerTypeAtSale;
        this.productId = product.getProductId();
        this.productName = product.getName();
        this.category = product.getCategory();
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.totalBeforeDiscount = product.getPrice() * quantity;
        this.finalPrice = finalPrice;
    }

    /**
     * Returns the unique identifier of this sale.
     *
     * @return the sale identifier, never {@code null}
     */
    public String getSaleId() {
        return saleId;
    }

    /**
     * Returns the moment the sale was completed.
     *
     * @return the time of the sale
     */
    public LocalDateTime getSaleTime() {
        return saleTime;
    }

    /**
     * Returns the branch the sale was made in.
     *
     * @return the branch, never {@code null}
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * Returns the employee number of the seller.
     *
     * @return the seller employee number
     */
    public String getSellerEmployeeNumber() {
        return sellerEmployeeNumber;
    }

    /**
     * Returns the identity number of the buying customer.
     *
     * @return the customer identity number
     */
    public String getCustomerIdNumber() {
        return customerIdNumber;
    }

    /**
     * Returns the kind of customer at the moment of the sale.
     *
     * @return the customer type as it was during the sale
     */
    public CustomerType getCustomerTypeAtSale() {
        return customerTypeAtSale;
    }

    /**
     * Returns the identifier of the product that was sold.
     *
     * @return the product identifier
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the name the product had at the moment of the sale.
     *
     * @return the product name
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Returns the category the product had at the moment of the sale.
     *
     * @return the product category
     */
    public ProductCategory getCategory() {
        return category;
    }

    /**
     * Returns how many items were sold.
     *
     * @return the quantity sold, always positive
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the catalogue price of one item at the moment of the sale.
     *
     * @return the unit price
     */
    public double getUnitPrice() {
        return unitPrice;
    }

    /**
     * Returns the total price before the discount of the purchase plan.
     *
     * @return the price before the discount
     */
    public double getTotalBeforeDiscount() {
        return totalBeforeDiscount;
    }

    /**
     * Returns the total price the customer actually paid.
     *
     * @return the final price
     */
    public double getFinalPrice() {
        return finalPrice;
    }

    /**
     * Returns the amount of money saved thanks to the purchase plan of the
     * customer.
     *
     * @return the difference between the price before the discount and the
     *         price actually paid
     */
    public double getDiscountAmount() {
        return totalBeforeDiscount - finalPrice;
    }

    /**
     * Compares sales by their unique identifier only.
     *
     * @param other the object to compare with
     * @return {@code true} if both objects describe the same sale
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Sale)) {
            return false;
        }
        Sale otherSale = (Sale) other;
        return saleId.equals(otherSale.saleId);
    }

    /**
     * Returns a hash code derived from the sale identifier.
     *
     * @return the hash code of the sale identifier
     */
    @Override
    public int hashCode() {
        return saleId.hashCode();
    }

    @Override
    public String toString() {
        return TimeUtil.formatForDisplay(saleTime) + " | " + branch.getDisplayName()
                + " | " + productName + " x" + quantity
                + " | paid " + finalPrice + " (before discount " + totalBeforeDiscount + ")"
                + " | seller " + sellerEmployeeNumber;
    }
}
