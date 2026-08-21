package server.chat;

import common.model.Branch;
import common.model.ChatMessage;
import common.model.ChatSessionInfo;
import common.model.Employee;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One open conversation between an employee of one branch and an employee of
 * another.
 * <p>
 * A session has exactly two owners - the employee who asked for the
 * conversation and the employee who answered - and it may also carry any number
 * of shift managers who joined it later. That asymmetry is deliberate: a
 * manager watches and may write, but closing the conversation belongs to the
 * two who are having it.
 * </p>
 * <p>
 * <b>Thread safety:</b> the messages and the joined managers are touched by the
 * client threads of every participant. Both collections are guarded by the
 * session object itself, and every block that touches them is a few lines long.
 * </p>
 */
public class ChatSession {

    /** The identifier of this session. */
    private final String sessionId;

    /** The employee who asked to open the conversation. */
    private final Employee initiator;

    /** The employee who answered the conversation. */
    private final Employee partner;

    /** The moment the conversation was opened. */
    private final LocalDateTime openedAt;

    /** Every message sent in this conversation, in order. */
    private final List<ChatMessage> messages = new ArrayList<>();

    /**
     * The shift managers who joined. A {@link LinkedHashSet} keeps the order
     * they joined in while making it impossible for the same manager to be
     * listed twice.
     */
    private final Set<String> joinedManagerNumbers = new LinkedHashSet<>();

    /** Whether the conversation is still open. */
    private volatile boolean open = true;

    /**
     * Opens a conversation between two employees.
     *
     * @param sessionId the identifier of the session
     * @param initiator the employee who asked for the conversation
     * @param partner   the employee who answered
     */
    public ChatSession(String sessionId, Employee initiator, Employee partner) {
        this.sessionId = sessionId;
        this.initiator = initiator;
        this.partner = partner;
        this.openedAt = LocalDateTime.now();
    }

    /**
     * Adds a message to the conversation.
     *
     * @param message the message that was sent
     */
    public void addMessage(ChatMessage message) {
        synchronized (this) {
            messages.add(message);
        }
    }

    /**
     * Returns every message sent so far.
     *
     * @return a copy of the message history, safe to send to a client
     */
    public List<ChatMessage> getMessageHistory() {
        synchronized (this) {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Records that a shift manager joined the conversation.
     *
     * @param managerEmployeeNumber the manager who joined
     * @return {@code true} if this manager was not already in the conversation
     */
    public boolean addManager(String managerEmployeeNumber) {
        synchronized (this) {
            return joinedManagerNumbers.add(managerEmployeeNumber);
        }
    }

    /**
     * Checks whether an employee takes part in this conversation, either as one
     * of the two owners or as a manager who joined.
     *
     * @param employeeNumber the employee to look for
     * @return {@code true} if that employee may read and write here
     */
    public boolean isParticipant(String employeeNumber) {
        if (initiator.getEmployeeNumber().equals(employeeNumber)
                || partner.getEmployeeNumber().equals(employeeNumber)) {
            return true;
        }
        synchronized (this) {
            return joinedManagerNumbers.contains(employeeNumber);
        }
    }

    /**
     * Returns the employee numbers of everybody who should receive a message of
     * this conversation.
     *
     * @return the two owners together with every manager who joined
     */
    public List<String> getAllParticipantNumbers() {
        List<String> participants = new ArrayList<>();
        participants.add(initiator.getEmployeeNumber());
        participants.add(partner.getEmployeeNumber());
        synchronized (this) {
            participants.addAll(joinedManagerNumbers);
        }
        return participants;
    }

    /**
     * Returns the other owner of the conversation.
     *
     * @param employeeNumber one of the two owners
     * @return the employee number of the other owner, or {@code null} when the
     *         given employee is not an owner
     */
    public String getOtherOwner(String employeeNumber) {
        if (initiator.getEmployeeNumber().equals(employeeNumber)) {
            return partner.getEmployeeNumber();
        }
        if (partner.getEmployeeNumber().equals(employeeNumber)) {
            return initiator.getEmployeeNumber();
        }
        return null;
    }

    /**
     * Builds the small description of this session that travels to a client.
     *
     * @return the description of this session
     */
    public ChatSessionInfo toSessionInfo() {
        List<String> managersCopy;
        synchronized (this) {
            managersCopy = new ArrayList<>(joinedManagerNumbers);
        }
        return new ChatSessionInfo(sessionId,
                initiator.getEmployeeNumber(), initiator.getFullName(), initiator.getBranch(),
                partner.getEmployeeNumber(), partner.getFullName(), partner.getBranch(),
                managersCopy, openedAt);
    }

    /**
     * Returns the identifier of this session.
     *
     * @return the session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the employee who asked for the conversation.
     *
     * @return the initiator
     */
    public Employee getInitiator() {
        return initiator;
    }

    /**
     * Returns the employee who answered the conversation.
     *
     * @return the partner
     */
    public Employee getPartner() {
        return partner;
    }

    /**
     * Returns the branches the two sides are sitting in, for the log.
     *
     * @return the two branches as text
     */
    public String describeBranches() {
        Branch initiatorBranch = initiator.getBranch();
        Branch partnerBranch = partner.getBranch();
        return initiatorBranch.getDisplayName() + " - " + partnerBranch.getDisplayName();
    }

    /**
     * Indicates whether the conversation is still open.
     *
     * @return {@code true} while the conversation is open
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Marks the conversation as closed.
     */
    public void close() {
        this.open = false;
    }

    @Override
    public String toString() {
        return "ChatSession " + sessionId + ": " + toSessionInfo().describeParticipants();
    }
}
