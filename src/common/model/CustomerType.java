package common.model;

/**
 * The three kinds of customer supported by the chain.
 * <p>
 * This enum is a <b>label</b> only. It exists so that the GUI can display the
 * customer kind and so that the {@link CustomerFactory} can decide which
 * concrete class to instantiate. The purchase calculation itself is never
 * chosen by inspecting this value - it is performed by a polymorphic call to
 * {@link Customer#calculateFinalPrice(double, int)}.
 * </p>
 */
public enum CustomerType {

    /** A customer who has not completed a purchase yet. */
    NEW("New Customer"),

    /** A customer who has already completed at least one purchase. */
    RETURNING("Returning Customer"),

    /** A loyal customer who passed the VIP threshold. */
    VIP("VIP Customer");

    /** The name presented to the user in the GUI. */
    private final String displayName;

    /**
     * Creates a customer type constant.
     *
     * @param displayName the human readable type name shown in the GUI
     */
    CustomerType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human readable customer type name.
     *
     * @return the display name of this type, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
