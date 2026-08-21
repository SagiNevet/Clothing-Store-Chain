package common.model;

/**
 * The product categories sold by the chain.
 * <p>
 * The category is a required field of {@link Product} and is one of the two
 * grouping keys of the sales report (the other one being the product itself).
 * </p>
 */
public enum ProductCategory {

    /** Shirts, t-shirts and blouses. */
    SHIRTS("Shirts"),

    /** Trousers, jeans and shorts. */
    PANTS("Pants"),

    /** Shoes, sandals and boots. */
    SHOES("Shoes"),

    /** Belts, hats, bags and other accessories. */
    ACCESSORIES("Accessories");

    /** The name presented to the user in the GUI. */
    private final String displayName;

    /**
     * Creates a category constant.
     *
     * @param displayName the human readable category name shown in the GUI
     */
    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human readable category name.
     *
     * @return the display name of this category, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
