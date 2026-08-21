package common.model;

import common.exception.InsufficientStockException;

import java.io.Serializable;

/**
 * A product in the inventory of a branch.
 * <p>
 * Each branch keeps its own inventory file, so the same product identifier can
 * appear in both branches with a different quantity in each one. The object
 * itself therefore does not carry a branch field - the file it was loaded from
 * is what determines the branch.
 * </p>
 * <p>
 * <b>Thread safety:</b> the quantity is changed by
 * {@link #decreaseQuantity(int)} and {@link #increaseQuantity(int)}. Both
 * methods are short and are always called from inside a {@code synchronized}
 * block in the inventory service, which locks this single product object. The
 * check "is there enough stock" and the subtraction that follows it must happen
 * inside the same lock, otherwise two employees selling the last item at the
 * same moment could both pass the check and drive the stock below zero.
 * </p>
 */
public class Product implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The smallest quantity a product may hold. */
    private static final int MINIMUM_QUANTITY = 0;

    /** The catalogue identifier of the product, and its unique key. */
    private final String productId;

    /** The name of the product as shown to the customer. */
    private String name;

    /** The category the product belongs to. */
    private ProductCategory category;

    /** The catalogue price of a single item. */
    private double price;

    /** How many items of this product the branch currently holds. */
    private int quantity;

    /**
     * Creates a product.
     *
     * @param productId the catalogue identifier, the unique key of a product
     * @param name      the name of the product
     * @param category  the category the product belongs to
     * @param price     the catalogue price of a single item, must not be negative
     * @param quantity  the amount currently in stock, must not be negative
     * @throws IllegalArgumentException if the price or the quantity is negative
     */
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

    /**
     * Removes items from the stock of this product, as part of a sale.
     * <p>
     * The check and the subtraction are written as one method on purpose: a
     * caller cannot accidentally ask "how much is left" and then subtract in a
     * separate step, which is exactly the pattern that creates a race
     * condition between two selling threads.
     * </p>
     *
     * @param amountToRemove how many items are being sold, must be positive
     * @throws InsufficientStockException if the branch holds fewer items than requested
     * @throws IllegalArgumentException   if {@code amountToRemove} is not positive
     */
    public void decreaseQuantity(int amountToRemove) throws InsufficientStockException {
        if (amountToRemove <= 0) {
            throw new IllegalArgumentException("amountToRemove must be positive: " + amountToRemove);
        }
        if (amountToRemove > quantity) {
            throw new InsufficientStockException(productId, amountToRemove, quantity);
        }
        quantity -= amountToRemove;
    }

    /**
     * Adds items to the stock of this product, as part of a purchase from a
     * supplier.
     *
     * @param amountToAdd how many items arrived, must be positive
     * @throws IllegalArgumentException if {@code amountToAdd} is not positive
     */
    public void increaseQuantity(int amountToAdd) {
        if (amountToAdd <= 0) {
            throw new IllegalArgumentException("amountToAdd must be positive: " + amountToAdd);
        }
        quantity += amountToAdd;
    }

    /**
     * Creates an independent copy of this product.
     * <p>
     * The server sends copies to the clients rather than the objects it keeps
     * in memory, so that a client can never hold a reference to a product that
     * another thread is about to change.
     * </p>
     *
     * @return a new product object holding the same values
     */
    public Product copy() {
        return new Product(productId, name, category, price, quantity);
    }

    /**
     * Returns the catalogue identifier, which is the unique key of a product.
     *
     * @return the product identifier, never {@code null}
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the name of the product.
     *
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the name of the product.
     *
     * @param name the new product name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the category of the product.
     *
     * @return the category, never {@code null}
     */
    public ProductCategory getCategory() {
        return category;
    }

    /**
     * Updates the category of the product.
     *
     * @param category the new category
     */
    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    /**
     * Returns the catalogue price of a single item.
     *
     * @return the unit price, never negative
     */
    public double getPrice() {
        return price;
    }

    /**
     * Updates the catalogue price of a single item.
     *
     * @param price the new unit price, must not be negative
     * @throws IllegalArgumentException if the price is negative
     */
    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative: " + price);
        }
        this.price = price;
    }

    /**
     * Returns how many items the branch currently holds.
     *
     * @return the quantity in stock, never negative
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Checks whether the branch can supply a requested amount.
     *
     * @param requestedQuantity the amount a seller wants to sell
     * @return {@code true} if the stock is large enough
     */
    public boolean hasEnoughStock(int requestedQuantity) {
        return quantity >= requestedQuantity;
    }

    /**
     * Compares products by their catalogue identifier only.
     *
     * @param other the object to compare with
     * @return {@code true} if both objects describe the same catalogue product
     */
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

    /**
     * Returns a hash code derived from the catalogue identifier.
     * <p>
     * Overridden together with {@link #equals(Object)} because the inventory of
     * a branch is kept in a {@code Map} keyed by product, and a set of products
     * is used when a report is filtered by several products at once.
     * </p>
     *
     * @return the hash code of the product identifier
     */
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
