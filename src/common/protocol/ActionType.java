package common.protocol;

public enum ActionType {

    LOGIN(false),

    LOGOUT(false),

    GET_INVENTORY(false),

    SELL_PRODUCT(false),

    RESTOCK_PRODUCT(false),

    ADD_PRODUCT(true),

    GET_CUSTOMERS(false),

    ADD_CUSTOMER(false),

    UPDATE_CUSTOMER(false),

    GET_EMPLOYEES(true),

    ADD_EMPLOYEE(true),

    GET_PASSWORD_POLICY(false),

    UPDATE_PASSWORD_POLICY(true),

    SALES_BY_BRANCH_REPORT(true),

    SALES_BY_PRODUCT_REPORT(true),

    EXPORT_REPORT(true),

    CHAT_REQUEST(false),

    CHAT_SEND(false),

    GET_ACTIVE_CHATS(true),

    CHAT_JOIN(true),

    CHAT_CLOSE(false),

    CHAT_CALLBACK(false);

    private final boolean shiftManagerOnly;

    ActionType(boolean shiftManagerOnly) {
        this.shiftManagerOnly = shiftManagerOnly;
    }

    public boolean isShiftManagerOnly() {
        return shiftManagerOnly;
    }
}
