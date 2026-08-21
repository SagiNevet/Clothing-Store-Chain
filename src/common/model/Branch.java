package common.model;

/**
 * The branches of the clothing store chain.
 * <p>
 * Every employee belongs to exactly one branch, and every branch keeps its own
 * separate inventory. This enum is used as a key for per-branch data structures
 * on the server, so its values must stay stable: renaming a value would break
 * previously saved data files whose names are derived from {@link #name()}.
 * </p>
 * <p>
 * Enum types are serializable by definition in Java, and they are serialized by
 * name rather than by object identity. That is exactly why an enum is a safer
 * choice than a plain String constant for a value that travels over the socket.
 * </p>
 */
public enum Branch {

    /** The Tel Aviv branch. */
    TEL_AVIV("Tel Aviv"),

    /** The Jerusalem branch. */
    JERUSALEM("Jerusalem");

    /** The name presented to the user in the GUI. */
    private final String displayName;

    /**
     * Creates a branch constant.
     *
     * @param displayName the human readable branch name shown in the GUI
     */
    Branch(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human readable branch name.
     *
     * @return the display name of this branch, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
