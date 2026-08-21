package common.model;

/**
 * The kinds of activity that are written to the log, one separate file per kind.
 * <p>
 * The requirement is that each kind of action is logged into its own file, so
 * the file name is a property of the enum constant itself. Adding a new kind of
 * log means adding one constant here and nothing else.
 * </p>
 */
public enum LogCategory {

    /** Creation and update of employee accounts. */
    EMPLOYEES("employees.log"),

    /** Creation and update of customers, including moves between customer kinds. */
    CUSTOMERS("customers.log"),

    /** Sales to customers and purchases of stock from suppliers. */
    SALES("sales.log"),

    /**
     * Chat sessions between branches. Always records who talked to whom, when
     * and from which branches; the content of the messages is recorded only
     * when the configuration flag for it is turned on.
     */
    CHAT("chat.log");

    /** The name of the file this category is written to. */
    private final String fileName;

    /**
     * Creates a log category.
     *
     * @param fileName the name of the file this category is written to
     */
    LogCategory(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the name of the file this category is written to.
     *
     * @return the log file name, never {@code null}
     */
    public String getFileName() {
        return fileName;
    }
}
