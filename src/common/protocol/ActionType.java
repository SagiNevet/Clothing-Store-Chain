package common.protocol;

/**
 * Every action a client can ask the server to perform.
 * <p>
 * The enum carries the permission rule together with the action: the flag
 * {@code shiftManagerOnly} states whether the action is restricted to a shift
 * manager. The server checks this single flag before executing any request, so
 * the permission rules live in one table instead of being repeated inside every
 * command class.
 * </p>
 * <p>
 * Sending an enum constant over the socket rather than a text command is what
 * makes the protocol safe: a typing mistake such as {@code "SEL_PRODUCT"} would
 * only be discovered at run time, while a wrong enum constant does not compile.
 * </p>
 */
public enum ActionType {

    /** Verify an employee number and password and open a session. */
    LOGIN(false),

    /** Close the session of the connected employee. */
    LOGOUT(false),

    /** Fetch the full inventory of the branch of the connected employee. */
    GET_INVENTORY(false),

    /** Sell items to a customer, applying the purchase plan of that customer. */
    SELL_PRODUCT(false),

    /** Add items to the stock of a product after a delivery from a supplier. */
    RESTOCK_PRODUCT(false),

    /** Add a brand new product to the catalogue of the branch. */
    ADD_PRODUCT(true),

    /** Fetch the customer list, which is shared by the whole chain. */
    GET_CUSTOMERS(false),

    /** Register a new customer of the chain. */
    ADD_CUSTOMER(false),

    /** Update the name or the phone number of an existing customer. */
    UPDATE_CUSTOMER(false),

    /** Fetch the employee list. */
    GET_EMPLOYEES(true),

    /** Create a new employee account. */
    ADD_EMPLOYEE(true),

    /** Fetch the current password policy. */
    GET_PASSWORD_POLICY(false),

    /** Replace the password policy of the system. */
    UPDATE_PASSWORD_POLICY(true),

    /** Build the report of the number of sales per branch. */
    SALES_BY_BRANCH_REPORT(true),

    /** Build the report of sales filtered by products or by categories. */
    SALES_BY_PRODUCT_REPORT(true),

    /** Write a report to a Word file on the server machine. */
    EXPORT_REPORT(true),

    /** Ask to open a chat with a free employee of another branch. */
    CHAT_REQUEST(false),

    /** Send a message inside an open chat session. */
    CHAT_SEND(false),

    /** Fetch the list of chat sessions currently open, in order to join one. */
    GET_ACTIVE_CHATS(true),

    /** Join a chat session that is already in progress. */
    CHAT_JOIN(true),

    /** Leave and close a chat session. */
    CHAT_CLOSE(false),

    /** Call back an employee whose earlier chat request could not be served. */
    CHAT_CALLBACK(false);

    /** Whether only a shift manager may perform this action. */
    private final boolean shiftManagerOnly;

    /**
     * Creates an action constant.
     *
     * @param shiftManagerOnly {@code true} if only a shift manager may perform it
     */
    ActionType(boolean shiftManagerOnly) {
        this.shiftManagerOnly = shiftManagerOnly;
    }

    /**
     * Indicates whether this action is restricted to a shift manager.
     *
     * @return {@code true} if only a shift manager may perform this action
     */
    public boolean isShiftManagerOnly() {
        return shiftManagerOnly;
    }
}
