package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One line of the log.
 * <p>
 * Every entry answers the same five questions: when it happened, who did it,
 * in which branch, what the action was, and any extra detail. Keeping the same
 * five fields in every category makes the log files easy to read side by side.
 * </p>
 * <p>
 * The entry is written as plain text and not as a serialized object, so that
 * the lecturer, or anybody else, can open the log with Notepad during the
 * defence without running the program.
 * </p>
 */
public class LogEntry implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The separator between the fields of a log line. */
    private static final String FIELD_SEPARATOR = " | ";

    /** The text used when an action was performed by the server itself. */
    private static final String SYSTEM_ACTOR = "SYSTEM";

    /** The category, which decides the file this entry is written to. */
    private final LogCategory category;

    /** The moment the action happened. */
    private final LocalDateTime eventTime;

    /** The employee number of whoever performed the action. */
    private final String actorEmployeeNumber;

    /** The branch the action was performed in, or {@code null} for the server itself. */
    private final Branch branch;

    /** A short name of the action, for example {@code SELL_PRODUCT}. */
    private final String action;

    /** Any extra detail worth keeping, for example the product and the price. */
    private final String details;

    /**
     * Creates a log entry.
     *
     * @param category            the category, which decides the target file
     * @param actorEmployeeNumber the employee number of whoever performed the
     *                            action, or {@code null} for the server itself
     * @param branch              the branch the action was performed in, may be {@code null}
     * @param action              a short name of the action
     * @param details             any extra detail worth keeping
     */
    public LogEntry(LogCategory category, String actorEmployeeNumber,
                    Branch branch, String action, String details) {
        this.category = category;
        this.eventTime = LocalDateTime.now();
        this.actorEmployeeNumber = actorEmployeeNumber;
        this.branch = branch;
        this.action = action;
        this.details = details;
    }

    /**
     * Returns the category of this entry.
     *
     * @return the log category, never {@code null}
     */
    public LogCategory getCategory() {
        return category;
    }

    /**
     * Returns the moment the action happened.
     *
     * @return the time of the event
     */
    public LocalDateTime getEventTime() {
        return eventTime;
    }

    /**
     * Returns the employee number of whoever performed the action.
     *
     * @return the actor employee number, or {@code null} for the server itself
     */
    public String getActorEmployeeNumber() {
        return actorEmployeeNumber;
    }

    /**
     * Returns the branch the action was performed in.
     *
     * @return the branch, or {@code null} when the action was not tied to a branch
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * Returns the short name of the action.
     *
     * @return the action name
     */
    public String getAction() {
        return action;
    }

    /**
     * Returns the extra details of the action.
     *
     * @return the details text
     */
    public String getDetails() {
        return details;
    }

    /**
     * Builds the single text line that is appended to the log file.
     *
     * @return the formatted log line, without a line separator at the end
     */
    public String toLogLine() {
        String actor = actorEmployeeNumber == null ? SYSTEM_ACTOR : actorEmployeeNumber;
        String branchName = branch == null ? SYSTEM_ACTOR : branch.getDisplayName();
        return TimeUtil.formatForDisplay(eventTime) + FIELD_SEPARATOR
                + actor + FIELD_SEPARATOR
                + branchName + FIELD_SEPARATOR
                + action + FIELD_SEPARATOR
                + details;
    }

    @Override
    public String toString() {
        return toLogLine();
    }
}
