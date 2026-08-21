package server.chat;

import common.model.Branch;
import common.model.Employee;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A chat request that could not be served, because nobody in the target branch
 * was free at that moment.
 * <p>
 * This is the entry the requirement calls "the list of users who did not
 * receive a conversation". The server keeps it, and when somebody in the target
 * branch becomes free the requester is notified and may call back.
 * </p>
 */
public class PendingChatRequest implements Serializable {

    /** Serialization version, because the request travels to the client. */
    private static final long serialVersionUID = 1L;

    /** The identifier of this waiting request. */
    private final String requestId;

    /** The employee number of whoever asked for the conversation. */
    private final String requesterEmployeeNumber;

    /** The name of whoever asked for the conversation. */
    private final String requesterName;

    /** The branch the requester is sitting in. */
    private final Branch requesterBranch;

    /** The branch the requester wanted to talk to. */
    private final Branch targetBranch;

    /** The moment the request was made. */
    private final LocalDateTime requestedAt;

    /**
     * Creates a waiting request.
     *
     * @param requestId    the identifier of this request
     * @param requester    the employee who asked for the conversation
     * @param targetBranch the branch that employee wanted to talk to
     */
    public PendingChatRequest(String requestId, Employee requester, Branch targetBranch) {
        this.requestId = requestId;
        this.requesterEmployeeNumber = requester.getEmployeeNumber();
        this.requesterName = requester.getFullName();
        this.requesterBranch = requester.getBranch();
        this.targetBranch = targetBranch;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * Returns the identifier of this request.
     *
     * @return the request identifier
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Returns the employee who asked for the conversation.
     *
     * @return the employee number of the requester
     */
    public String getRequesterEmployeeNumber() {
        return requesterEmployeeNumber;
    }

    /**
     * Returns the name of the employee who asked for the conversation.
     *
     * @return the name of the requester
     */
    public String getRequesterName() {
        return requesterName;
    }

    /**
     * Returns the branch the requester is sitting in.
     *
     * @return the branch of the requester
     */
    public Branch getRequesterBranch() {
        return requesterBranch;
    }

    /**
     * Returns the branch the requester wanted to talk to.
     *
     * @return the target branch
     */
    public Branch getTargetBranch() {
        return targetBranch;
    }

    /**
     * Returns the moment the request was made.
     *
     * @return the time of the request
     */
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return requesterName + " of " + requesterBranch.getDisplayName()
                + " is waiting to talk to " + targetBranch.getDisplayName();
    }
}
