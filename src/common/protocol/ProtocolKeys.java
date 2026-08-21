package common.protocol;

/**
 * The names of the values carried inside the payload of a request, a response
 * or an event.
 * <p>
 * A request carries a {@code Map} of parameters, which is flexible enough for
 * more than twenty different actions. The danger of a map is the loose text
 * key: writing {@code "quantity"} on the client and reading {@code "quantitiy"}
 * on the server compiles perfectly and fails only at run time. Collecting every
 * key here as a constant removes that danger, because both sides now use the
 * same field and a typing mistake becomes a compilation error.
 * </p>
 */
public final class ProtocolKeys {

    /** The employee number typed in the login screen. */
    public static final String EMPLOYEE_NUMBER = "employeeNumber";

    /** The password typed in the login screen. */
    public static final String PASSWORD = "password";

    /** An {@code Employee} object. */
    public static final String EMPLOYEE = "employee";

    /** A {@code List} of {@code Employee} objects. */
    public static final String EMPLOYEE_LIST = "employeeList";

    /** A {@code Branch} constant. */
    public static final String BRANCH = "branch";

    /** The identifier of a product. */
    public static final String PRODUCT_ID = "productId";

    /** A {@code Product} object. */
    public static final String PRODUCT = "product";

    /** A {@code List} of {@code Product} objects. */
    public static final String PRODUCT_LIST = "productList";

    /** A number of items. */
    public static final String QUANTITY = "quantity";

    /** The identity number of a customer. */
    public static final String CUSTOMER_ID = "customerId";

    /** A {@code Customer} object. */
    public static final String CUSTOMER = "customer";

    /** A {@code List} of {@code Customer} objects. */
    public static final String CUSTOMER_LIST = "customerList";

    /** The full name of a person. */
    public static final String FULL_NAME = "fullName";

    /** The phone number of a person. */
    public static final String PHONE = "phone";

    /** A {@code Sale} object. */
    public static final String SALE = "sale";

    /** A {@code PasswordPolicy} object. */
    public static final String PASSWORD_POLICY = "passwordPolicy";

    /** The kind of report requested. */
    public static final String REPORT_TYPE = "reportType";

    /** A {@code List} of the rows of a report. */
    public static final String REPORT_ROWS = "reportRows";

    /** The name of the format a report should be exported to. */
    public static final String EXPORT_FORMAT = "exportFormat";

    /** The full path of a file that was written on the server machine. */
    public static final String FILE_PATH = "filePath";

    /** A {@code List} of the categories a report is filtered by. */
    public static final String CATEGORY_FILTER = "categoryFilter";

    /** A {@code List} of the products a report is filtered by. */
    public static final String PRODUCT_FILTER = "productFilter";

    /** The identifier of a chat session. */
    public static final String CHAT_SESSION_ID = "chatSessionId";

    /** The identifier of a chat request waiting in the queue. */
    public static final String CHAT_REQUEST_ID = "chatRequestId";

    /** A {@code ChatSessionInfo} object describing one conversation. */
    public static final String CHAT_SESSION = "chatSession";

    /** A {@code List} of {@code ChatSessionInfo} objects. */
    public static final String CHAT_SESSION_LIST = "chatSessionList";

    /** A {@code ChatMessage} object. */
    public static final String CHAT_MESSAGE = "chatMessage";

    /** A {@code List} of {@code ChatMessage} objects, the history of a session. */
    public static final String CHAT_HISTORY = "chatHistory";

    /** The text of a chat message. */
    public static final String MESSAGE_TEXT = "messageText";

    /** The branch a chat is requested with. */
    public static final String TARGET_BRANCH = "targetBranch";

    /** A flag telling the client that its chat request is waiting in the queue. */
    public static final String QUEUED = "queued";

    /**
     * Prevents instantiation. This class only holds constants.
     */
    private ProtocolKeys() {
    }
}
