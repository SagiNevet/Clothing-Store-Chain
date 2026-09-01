package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Sale implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String saleId;

    private final LocalDateTime saleTime;

    private final Branch branch;

    private final String sellerEmployeeNumber;

    private final String customerIdNumber;

    private final CustomerType customerTypeAtSale;

    private final String productId;

    private final String productName;

    private final ProductCategory category;

    private final int quantity;

    private final double unitPrice;

    private final double totalBeforeDiscount;

    private final double finalPrice;

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

    public String getSaleId() {
        return saleId;
    }

    public LocalDateTime getSaleTime() {
        return saleTime;
    }

    public Branch getBranch() {
        return branch;
    }

    public String getSellerEmployeeNumber() {
        return sellerEmployeeNumber;
    }

    public String getCustomerIdNumber() {
        return customerIdNumber;
    }

    public CustomerType getCustomerTypeAtSale() {
        return customerTypeAtSale;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalBeforeDiscount() {
        return totalBeforeDiscount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public double getDiscountAmount() {
        return totalBeforeDiscount - finalPrice;
    }

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
