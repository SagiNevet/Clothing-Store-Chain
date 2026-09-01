package common.model;

public enum Role {

    SHIFT_MANAGER("Shift Manager", true, true, true),

    CASHIER("Cashier", false, false, false),

    SELLER("Seller", false, false, false);

    private final String displayName;

    private final boolean allowedToManageEmployees;

    private final boolean allowedToViewReports;

    private final boolean allowedToJoinExistingChat;

    Role(String displayName,
         boolean allowedToManageEmployees,
         boolean allowedToViewReports,
         boolean allowedToJoinExistingChat) {
        this.displayName = displayName;
        this.allowedToManageEmployees = allowedToManageEmployees;
        this.allowedToViewReports = allowedToViewReports;
        this.allowedToJoinExistingChat = allowedToJoinExistingChat;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canManageEmployees() {
        return allowedToManageEmployees;
    }

    public boolean canViewReports() {
        return allowedToViewReports;
    }

    public boolean canJoinExistingChat() {
        return allowedToJoinExistingChat;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
