package common.model;

public enum CustomerType {

    NEW("New Customer"),

    RETURNING("Returning Customer"),

    VIP("VIP Customer");

    private final String displayName;

    CustomerType(String displayName) {
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
