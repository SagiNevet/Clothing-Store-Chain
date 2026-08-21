package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A description of one chat session, as it is shown to a client.
 * <p>
 * The session itself lives on the server; this is the small, read only picture
 * of it that travels over the socket. A shift manager choosing which
 * conversation to join sees a list of these.
 * </p>
 */
public class ChatSessionInfo implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The identifier of the session. */
    private final String sessionId;

    /** The employee who asked to open the conversation. */
    private final String initiatorEmployeeNumber;

    /** The name of the employee who asked to open the conversation. */
    private final String initiatorName;

    /** The branch the initiator is sitting in. */
    private final Branch initiatorBranch;

    /** The employee who answered the conversation. */
    private final String partnerEmployeeNumber;

    /** The name of the employee who answered the conversation. */
    private final String partnerName;

    /** The branch the partner is sitting in. */
    private final Branch partnerBranch;

    /** The employee numbers of the shift managers who joined, if any. */
    private final List<String> joinedManagers;

    /** The moment the conversation was opened. */
    private final LocalDateTime openedAt;

    /**
     * Creates the description of a session.
     *
     * @param sessionId               the identifier of the session
     * @param initiatorEmployeeNumber the employee who opened the conversation
     * @param initiatorName           the name of that employee
     * @param initiatorBranch         the branch that employee is sitting in
     * @param partnerEmployeeNumber   the employee who answered
     * @param partnerName             the name of that employee
     * @param partnerBranch           the branch that employee is sitting in
     * @param joinedManagers          the managers who joined the conversation
     * @param openedAt                the moment the conversation was opened
     */
    public ChatSessionInfo(String sessionId, String initiatorEmployeeNumber,
                           String initiatorName, Branch initiatorBranch,
                           String partnerEmployeeNumber, String partnerName,
                           Branch partnerBranch, List<String> joinedManagers,
                           LocalDateTime openedAt) {
        this.sessionId = sessionId;
        this.initiatorEmployeeNumber = initiatorEmployeeNumber;
        this.initiatorName = initiatorName;
        this.initiatorBranch = initiatorBranch;
        this.partnerEmployeeNumber = partnerEmployeeNumber;
        this.partnerName = partnerName;
        this.partnerBranch = partnerBranch;
        this.joinedManagers = Collections.unmodifiableList(new ArrayList<>(joinedManagers));
        this.openedAt = openedAt;
    }

    /**
     * Returns the identifier of the session.
     *
     * @return the session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the employee who opened the conversation.
     *
     * @return the employee number of the initiator
     */
    public String getInitiatorEmployeeNumber() {
        return initiatorEmployeeNumber;
    }

    /**
     * Returns the name of the employee who opened the conversation.
     *
     * @return the name of the initiator
     */
    public String getInitiatorName() {
        return initiatorName;
    }

    /**
     * Returns the branch of the employee who opened the conversation.
     *
     * @return the branch of the initiator
     */
    public Branch getInitiatorBranch() {
        return initiatorBranch;
    }

    /**
     * Returns the employee who answered the conversation.
     *
     * @return the employee number of the partner
     */
    public String getPartnerEmployeeNumber() {
        return partnerEmployeeNumber;
    }

    /**
     * Returns the name of the employee who answered the conversation.
     *
     * @return the name of the partner
     */
    public String getPartnerName() {
        return partnerName;
    }

    /**
     * Returns the branch of the employee who answered the conversation.
     *
     * @return the branch of the partner
     */
    public Branch getPartnerBranch() {
        return partnerBranch;
    }

    /**
     * Returns the shift managers who joined this conversation.
     *
     * @return an unmodifiable list of employee numbers, possibly empty
     */
    public List<String> getJoinedManagers() {
        return joinedManagers;
    }

    /**
     * Returns the moment the conversation was opened.
     *
     * @return the opening time
     */
    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    /**
     * Builds the description used in the log and in the list of open sessions.
     *
     * @return a sentence naming both sides and their branches
     */
    public String describeParticipants() {
        return initiatorName + " (" + initiatorBranch.getDisplayName() + ")"
                + " with " + partnerName + " (" + partnerBranch.getDisplayName() + ")";
    }

    @Override
    public String toString() {
        return describeParticipants() + ", opened at " + TimeUtil.formatForDisplay(openedAt)
                + (joinedManagers.isEmpty() ? "" : ", joined by " + joinedManagers);
    }
}
