package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

public class LogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_SEPARATOR = " | ";

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final LogCategory category;

    private final LocalDateTime eventTime;

    private final String actorEmployeeNumber;

    private final Branch branch;

    private final String action;

    private final String details;

    public LogEntry(LogCategory category, String actorEmployeeNumber,
                    Branch branch, String action, String details) {
        this.category = category;
        this.eventTime = LocalDateTime.now();
        this.actorEmployeeNumber = actorEmployeeNumber;
        this.branch = branch;
        this.action = action;
        this.details = details;
    }

    public LogCategory getCategory() {
        return category;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public String getActorEmployeeNumber() {
        return actorEmployeeNumber;
    }

    public Branch getBranch() {
        return branch;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

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
