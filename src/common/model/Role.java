package common.model;

/**
 * The single job role held by an employee.
 * <p>
 * An employee holds exactly one role - never a combination of roles. The
 * permissions of the system are derived from the role instead of being written
 * as scattered {@code if} statements across the code: every permission is a
 * field of the enum constant, so adding a new role or changing a permission is
 * a one line change in this file only.
 * </p>
 */
public enum Role {

    /**
     * Shift manager. The only role allowed to join an existing chat session,
     * to manage employee accounts and to view the sales reports.
     */
    SHIFT_MANAGER("Shift Manager", true, true, true),

    /** Cashier. Sells and restocks products, manages customers, chats normally. */
    CASHIER("Cashier", false, false, false),

    /** Seller. Same permissions as a cashier. */
    SELLER("Seller", false, false, false);

    /** The name presented to the user in the GUI. */
    private final String displayName;

    /** Whether this role may create and edit employee accounts. */
    private final boolean allowedToManageEmployees;

    /** Whether this role may generate and export the sales reports. */
    private final boolean allowedToViewReports;

    /** Whether this role may join a chat session that is already open. */
    private final boolean allowedToJoinExistingChat;

    /**
     * Creates a role constant together with the permissions it grants.
     *
     * @param displayName               the human readable role name
     * @param allowedToManageEmployees  {@code true} if the role may manage employee accounts
     * @param allowedToViewReports      {@code true} if the role may view and export reports
     * @param allowedToJoinExistingChat {@code true} if the role may join an open chat session
     */
    Role(String displayName,
         boolean allowedToManageEmployees,
         boolean allowedToViewReports,
         boolean allowedToJoinExistingChat) {
        this.displayName = displayName;
        this.allowedToManageEmployees = allowedToManageEmployees;
        this.allowedToViewReports = allowedToViewReports;
        this.allowedToJoinExistingChat = allowedToJoinExistingChat;
    }

    /**
     * Returns the human readable role name.
     *
     * @return the display name of this role, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indicates whether this role may create and edit employee accounts and
     * change the password policy.
     *
     * @return {@code true} for {@link #SHIFT_MANAGER}, {@code false} otherwise
     */
    public boolean canManageEmployees() {
        return allowedToManageEmployees;
    }

    /**
     * Indicates whether this role may generate and export sales reports.
     *
     * @return {@code true} for {@link #SHIFT_MANAGER}, {@code false} otherwise
     */
    public boolean canViewReports() {
        return allowedToViewReports;
    }

    /**
     * Indicates whether this role may join a chat session that is already in
     * progress between two other employees.
     *
     * @return {@code true} for {@link #SHIFT_MANAGER}, {@code false} otherwise
     */
    public boolean canJoinExistingChat() {
        return allowedToJoinExistingChat;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
