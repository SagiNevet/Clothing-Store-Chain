package common.model;

import common.exception.InsufficientStockException;

import java.io.Serializable;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MINIMUM_QUANTITY = 0;

    private final String productId;

    private String name;

    private ProductCategory category;

    private double price;

    private int quantity;

    public Product(String productId, String name, ProductCategory category,
                   double price, int quantity) {
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative: " + price);
        }
        if (quantity < MINIMUM_QUANTITY) {
            throw new IllegalArgumentException("quantity must not be negative: " + quantity);
        }
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
    }

    public void decreaseQuantity(int amountToRemove) throws InsufficientStockException {
        if (amountToRemove <= 0) {
            throw new IllegalArgumentException("amountToRemove must be positive: " + amountToRemove);
        }
        if (amountToRemove > quantity) {
            throw new InsufficientStockException(productId, amountToRemove, quantity);
        }
        quantity -= amountToRemove;
    }

    public void increaseQuantity(int amountToAdd) {
        if (amountToAdd <= 0) {
            throw new IllegalArgumentException("amountToAdd must be positive: " + amountToAdd);
        }
        quantity += amountToAdd;
    }

    public Product copy() {
        return new Product(productId, name, category, price, quantity);
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative: " + price);
        }
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean hasEnoughStock(int requestedQuantity) {
        return quantity >= requestedQuantity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product)) {
            return false;
        }
        Product otherProduct = (Product) other;
        return productId.equals(otherProduct.productId);
    }

    @Override
    public int hashCode() {
        return productId.hashCode();
    }

    @Override
    public String toString() {
        return productId + " - " + name + " (" + category.getDisplayName()
                + ", price " + price + ", in stock " + quantity + ")";
    }
}
