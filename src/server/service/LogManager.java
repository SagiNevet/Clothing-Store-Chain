package server.service;

import common.model.Branch;
import common.model.LogCategory;
import common.model.LogEntry;
import common.util.AppConfig;
import server.storage.LogWriter;

/**
 * The single entry point for writing to the log files.
 * <p>
 * The class is a <b>Singleton</b>, so the whole server shares one
 * {@link LogWriter} and therefore one set of locks. Two writers would each hold
 * their own locks, and two threads could interleave their lines inside the same
 * file.
 * </p>
 * <p>
 * The methods are named after the four categories required by the project, so a
 * caller never has to remember which file an action belongs to.
 * </p>
 */
public final class LogManager {

    /** The single instance, created when the class is first loaded. */
    private static final LogManager INSTANCE = new LogManager();

    /** Configuration key deciding whether chat message text is written to the log. */
    private static final String CONFIG_KEY_SAVE_CHAT_CONTENT = "chat.saveMessageContent";

    /** Whether chat content is saved when the configuration file is missing. */
    private static final boolean DEFAULT_SAVE_CHAT_CONTENT = false;

    /** The text written instead of a message when saving content is turned off. */
    private static final String CONTENT_HIDDEN = "(message content not saved)";

    /** The object that actually appends the lines to the files. */
    private final LogWriter logWriter = new LogWriter();

    /**
     * Prevents anybody from building a second log manager.
     */
    private LogManager() {
    }

    /**
     * Returns the single log manager instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static LogManager getInstance() {
        return INSTANCE;
    }

    /**
     * Writes a line to the employees log.
     *
     * @param actorEmployeeNumber who performed the action, or {@code null} for the server
     * @param branch              the branch the action happened in, may be {@code null}
     * @param action              a short name of the action
     * @param details             any extra detail worth keeping
     */
    public void logEmployeeAction(String actorEmployeeNumber, Branch branch,
                                  String action, String details) {
        logWriter.write(new LogEntry(LogCategory.EMPLOYEES, actorEmployeeNumber,
                branch, action, details));
    }

    /**
     * Writes a line to the customers log.
     *
     * @param actorEmployeeNumber who performed the action
     * @param branch              the branch the action happened in
     * @param action              a short name of the action
     * @param details             any extra detail worth keeping
     */
    public void logCustomerAction(String actorEmployeeNumber, Branch branch,
                                  String action, String details) {
        logWriter.write(new LogEntry(LogCategory.CUSTOMERS, actorEmployeeNumber,
                branch, action, details));
    }

    /**
     * Writes a line to the sales log.
     *
     * @param actorEmployeeNumber who performed the sale or the restock
     * @param branch              the branch the action happened in
     * @param action              a short name of the action
     * @param details             any extra detail worth keeping
     */
    public void logSalesAction(String actorEmployeeNumber, Branch branch,
                               String action, String details) {
        logWriter.write(new LogEntry(LogCategory.SALES, actorEmployeeNumber,
                branch, action, details));
    }

    /**
     * Writes a line to the chat log.
     * <p>
     * Who talked to whom, when and from which branches is <b>always</b>
     * recorded. The text of the message is recorded only when the configuration
     * flag {@code chat.saveMessageContent} is turned on, exactly as the
     * requirement asks.
     * </p>
     *
     * @param actorEmployeeNumber the employee who sent the message or opened the session
     * @param branch              the branch that employee is sitting in
     * @param action              a short name of the action, for example {@code CHAT_MESSAGE}
     * @param participants        a description of the two sides of the conversation
     * @param messageContent      the text of the message, may be {@code null}
     */
    public void logChatAction(String actorEmployeeNumber, Branch branch, String action,
                              String participants, String messageContent) {
        String details = participants;
        if (messageContent != null) {
            details = details + " -> " + (isChatContentSaved()
                    ? messageContent
                    : CONTENT_HIDDEN);
        }
        logWriter.write(new LogEntry(LogCategory.CHAT, actorEmployeeNumber,
                branch, action, details));
    }

    /**
     * Indicates whether the text of chat messages is written to the log.
     *
     * @return {@code true} when the configuration flag is turned on
     */
    public boolean isChatContentSaved() {
        return AppConfig.getInstance().getBoolean(
                CONFIG_KEY_SAVE_CHAT_CONTENT, DEFAULT_SAVE_CHAT_CONTENT);
    }
}
