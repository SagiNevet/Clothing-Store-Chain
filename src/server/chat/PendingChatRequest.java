package server.chat;

import common.model.Branch;
import common.model.Employee;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PendingChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String requestId;

    private final String requesterEmployeeNumber;

    private final String requesterName;

    private final Branch requesterBranch;

    private final Branch targetBranch;

    private final LocalDateTime requestedAt;

    public PendingChatRequest(String requestId, Employee requester, Branch targetBranch) {
        this.requestId = requestId;
        this.requesterEmployeeNumber = requester.getEmployeeNumber();
        this.requesterName = requester.getFullName();
        this.requesterBranch = requester.getBranch();
        this.targetBranch = targetBranch;
        this.requestedAt = LocalDateTime.now();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequesterEmployeeNumber() {
        return requesterEmployeeNumber;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public Branch getRequesterBranch() {
        return requesterBranch;
    }

    public Branch getTargetBranch() {
        return targetBranch;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return requesterName + " of " + requesterBranch.getDisplayName()
                + " is waiting to talk to " + targetBranch.getDisplayName();
    }
}
