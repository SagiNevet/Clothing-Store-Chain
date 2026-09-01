package common.model;

public enum ProductCategory {

    SHIRTS("Shirts"),

    PANTS("Pants"),

    SHOES("Shoes"),

    ACCESSORIES("Accessories");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
