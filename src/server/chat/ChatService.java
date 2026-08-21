package server.chat;

import common.exception.ChatUnavailableException;
import common.exception.EntityNotFoundException;
import common.exception.PermissionDeniedException;
import common.model.Branch;
import common.model.ChatMessage;
import common.model.ChatSessionInfo;
import common.model.Employee;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.ServerEvent;
import common.util.IdGenerator;
import server.observer.EventPublisher;
import server.service.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the chat feature: opens conversations, delivers messages, lets a shift
 * manager join, and closes conversations.
 * <p>
 * The service is the layer between the commands and
 * {@link ChatQueueManager}. The queue manager answers one question only - who
 * is free - while this class owns the conversations themselves and everything
 * that has to be told to the participants.
 * </p>
 * <p>
 * <b>Two rules of the requirements are enforced here and nowhere else:</b>
 * </p>
 * <ul>
 *   <li>Only a shift manager may join a conversation that is already open.</li>
 *   <li>The same employee never holds two conversations at once, which the
 *       queue manager guarantees by marking them busy.</li>
 * </ul>
 */
public final class ChatService {

    /** The single instance, created when the class is first loaded. */
    private static final ChatService INSTANCE = new ChatService();

    /** Every open conversation, keyed by session identifier. */
    private final Map<String, ChatSession> openSessions = new ConcurrentHashMap<>();

    /**
     * Prevents anybody from building a second chat service.
     */
    private ChatService() {
    }

    /**
     * Returns the single chat service instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static ChatService getInstance() {
        return INSTANCE;
    }

    /**
     * Opens a conversation with a free employee of another branch.
     *
     * @param requester    the employee asking for the conversation
     * @param targetBranch the branch to talk to
     * @return the conversation that was opened
     * @throws ChatUnavailableException if the requester is already in a
     *                                  conversation, or if nobody was free and
     *                                  the request went into the queue
     */
    public ChatSession openChat(Employee requester, Branch targetBranch)
            throws ChatUnavailableException {
        ChatQueueManager queueManager = ChatQueueManager.getInstance();

        if (queueManager.isBusy(requester.getEmployeeNumber())) {
            throw new ChatUnavailableException(
                    "You are already in a conversation. Close it before opening another one.",
                    false);
        }

        String pendingId = IdGenerator.nextChatSessionId();
        PendingChatRequest pendingRequest =
                new PendingChatRequest(pendingId, requester, targetBranch);

        Employee partner;
        try {
            partner = queueManager.findPartnerOrQueue(requester, targetBranch, pendingRequest);
        } catch (InterruptedException waitInterrupted) {
            Thread.currentThread().interrupt();
            throw new ChatUnavailableException(
                    "The wait for a free employee was interrupted", false);
        }

        if (partner == null) {
            LogManager.getInstance().logChatAction(requester.getEmployeeNumber(),
                    requester.getBranch(), "CHAT_QUEUED",
                    "Waiting for a free employee of " + targetBranch.getDisplayName(), null);
            throw new ChatUnavailableException("Nobody in "
                    + targetBranch.getDisplayName() + " is free right now. "
                    + "You are in the queue and will be notified when somebody is available.",
                    true);
        }

        ChatSession session = new ChatSession(pendingId, requester, partner);
        openSessions.put(session.getSessionId(), session);

        LogManager.getInstance().logChatAction(requester.getEmployeeNumber(),
                requester.getBranch(), "CHAT_OPENED",
                session.toSessionInfo().describeParticipants(), null);

        // The employee who was chosen is told that a conversation was opened
        // with them, so their window can open the chat tab by itself.
        EventPublisher.getInstance().publishToEmployee(partner.getEmployeeNumber(),
                new ServerEvent(EventType.CHAT_INVITE)
                        .withPayload(ProtocolKeys.CHAT_SESSION_ID, session.getSessionId())
                        .withPayload(ProtocolKeys.CHAT_SESSION, session.toSessionInfo()));

        return session;
    }

    /**
     * Sends a message inside an open conversation.
     *
     * @param sender    the employee writing the message
     * @param sessionId the conversation to write in
     * @param text      the text of the message
     * @return the message that was stored
     * @throws EntityNotFoundException    if there is no such open conversation
     * @throws PermissionDeniedException  if the sender does not take part in it
     */
    public ChatMessage sendMessage(Employee sender, String sessionId, String text)
            throws EntityNotFoundException, PermissionDeniedException {
        ChatSession session = requireSession(sessionId);

        if (!session.isParticipant(sender.getEmployeeNumber())) {
            throw new PermissionDeniedException(sender.getRole(),
                    "writing in a conversation they are not part of");
        }

        ChatMessage message = new ChatMessage(sessionId, sender.getEmployeeNumber(),
                sender.getFullName(), sender.getBranch(), text);
        session.addMessage(message);

        // The participants list is always recorded. The text itself is recorded
        // only when the configuration flag asks for it.
        LogManager.getInstance().logChatAction(sender.getEmployeeNumber(), sender.getBranch(),
                "CHAT_MESSAGE", session.toSessionInfo().describeParticipants(), text);

        deliverToParticipants(session, new ServerEvent(EventType.CHAT_MESSAGE)
                .withPayload(ProtocolKeys.CHAT_SESSION_ID, sessionId)
                .withPayload(ProtocolKeys.CHAT_MESSAGE, message));

        return message;
    }

    /**
     * Lets a shift manager join a conversation that is already open.
     *
     * @param manager   the shift manager who wants to join
     * @param sessionId the conversation to join
     * @return the history of the conversation so far
     * @throws EntityNotFoundException   if there is no such open conversation
     * @throws PermissionDeniedException if the role is not a shift manager
     */
    public List<ChatMessage> joinChat(Employee manager, String sessionId)
            throws EntityNotFoundException, PermissionDeniedException {
        // The rule of the requirements: a cashier or a seller may never join an
        // existing conversation. The check lives here, in the service, so that it
        // holds for every path that could ever reach this feature.
        if (!manager.getRole().canJoinExistingChat()) {
            throw new PermissionDeniedException(manager.getRole(),
                    "joining a conversation that is already open");
        }

        ChatSession session = requireSession(sessionId);
        boolean joinedNow = session.addManager(manager.getEmployeeNumber());

        if (joinedNow) {
            LogManager.getInstance().logChatAction(manager.getEmployeeNumber(),
                    manager.getBranch(), "CHAT_JOINED",
                    session.toSessionInfo().describeParticipants(), null);

            deliverToParticipants(session, new ServerEvent(EventType.CHAT_MANAGER_JOINED)
                    .withPayload(ProtocolKeys.CHAT_SESSION_ID, sessionId)
                    .withPayload(ProtocolKeys.FULL_NAME, manager.getFullName()));
        }

        return session.getMessageHistory();
    }

    /**
     * Closes a conversation and frees both employees.
     *
     * @param closer    the employee closing the conversation
     * @param sessionId the conversation to close
     * @throws EntityNotFoundException   if there is no such open conversation
     * @throws PermissionDeniedException if the employee does not take part in it
     */
    public void closeChat(Employee closer, String sessionId)
            throws EntityNotFoundException, PermissionDeniedException {
        ChatSession session = requireSession(sessionId);

        if (!session.isParticipant(closer.getEmployeeNumber())) {
            throw new PermissionDeniedException(closer.getRole(),
                    "closing a conversation they are not part of");
        }

        session.close();
        openSessions.remove(sessionId);

        LogManager.getInstance().logChatAction(closer.getEmployeeNumber(), closer.getBranch(),
                "CHAT_CLOSED", session.toSessionInfo().describeParticipants(), null);

        deliverToParticipants(session, new ServerEvent(EventType.CHAT_CLOSED)
                .withPayload(ProtocolKeys.CHAT_SESSION_ID, sessionId));

        // Both owners are free again, and everybody waiting for their branch is
        // told so. A manager who joined was never marked busy, because watching
        // a conversation does not stop them from being called.
        releaseOwner(session.getInitiator());
        releaseOwner(session.getPartner());
    }

    /**
     * Frees one employee and notifies whoever was waiting for their branch.
     *
     * @param employee the employee who is available again
     */
    public void releaseOwner(Employee employee) {
        ChatQueueManager queueManager = ChatQueueManager.getInstance();
        queueManager.markFree(employee.getEmployeeNumber());

        // Everybody who asked to talk to this branch and got no answer is told
        // that somebody is available now. This is the notification the
        // requirement asks for: the requester may call back.
        List<PendingChatRequest> waitingRequests =
                queueManager.takeRequestsWaitingFor(employee.getBranch());
        for (PendingChatRequest waitingRequest : waitingRequests) {
            EventPublisher.getInstance().publishToEmployee(
                    waitingRequest.getRequesterEmployeeNumber(),
                    new ServerEvent(EventType.CHAT_PEER_AVAILABLE)
                            .withPayload(ProtocolKeys.CHAT_REQUEST_ID,
                                    waitingRequest.getRequestId())
                            .withPayload(ProtocolKeys.TARGET_BRANCH,
                                    waitingRequest.getTargetBranch())
                            .withPayload(ProtocolKeys.FULL_NAME, employee.getFullName()));

            LogManager.getInstance().logChatAction(employee.getEmployeeNumber(),
                    employee.getBranch(), "CHAT_PEER_AVAILABLE",
                    "Told " + waitingRequest.getRequesterName()
                            + " that " + employee.getFullName() + " is free", null);
        }
    }

    /**
     * Closes every conversation of an employee who disconnected, and drops any
     * request they were waiting on.
     *
     * @param employee the employee whose connection was lost
     */
    public void handleDisconnection(Employee employee) {
        if (employee == null) {
            return;
        }
        ChatQueueManager.getInstance().removeRequestsOf(employee.getEmployeeNumber());

        for (ChatSession session : new ArrayList<>(openSessions.values())) {
            if (session.getInitiator().getEmployeeNumber().equals(employee.getEmployeeNumber())
                    || session.getPartner().getEmployeeNumber()
                    .equals(employee.getEmployeeNumber())) {
                openSessions.remove(session.getSessionId());
                session.close();

                deliverToParticipants(session, new ServerEvent(EventType.CHAT_CLOSED)
                        .withPayload(ProtocolKeys.CHAT_SESSION_ID, session.getSessionId()));

                LogManager.getInstance().logChatAction(employee.getEmployeeNumber(),
                        employee.getBranch(), "CHAT_CLOSED_BY_DISCONNECT",
                        session.toSessionInfo().describeParticipants(), null);

                releaseOwner(session.getInitiator());
                releaseOwner(session.getPartner());
            }
        }
        ChatQueueManager.getInstance().markFree(employee.getEmployeeNumber());
    }

    /**
     * Returns the conversations that are open right now.
     *
     * @return the description of every open conversation
     */
    public List<ChatSessionInfo> getOpenSessions() {
        List<ChatSessionInfo> descriptions = new ArrayList<>();
        for (ChatSession session : openSessions.values()) {
            descriptions.add(session.toSessionInfo());
        }
        return descriptions;
    }

    /**
     * Finds an open conversation or fails.
     *
     * @param sessionId the conversation to find
     * @return the open conversation
     * @throws EntityNotFoundException if it does not exist or was already closed
     */
    private ChatSession requireSession(String sessionId) throws EntityNotFoundException {
        ChatSession session = openSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            throw new EntityNotFoundException("Chat session", String.valueOf(sessionId));
        }
        return session;
    }

    /**
     * Sends an event to everybody taking part in a conversation.
     *
     * @param session the conversation
     * @param event   the event to send
     */
    private void deliverToParticipants(ChatSession session, ServerEvent event) {
        EventPublisher publisher = EventPublisher.getInstance();
        for (String participantNumber : session.getAllParticipantNumbers()) {
            publisher.publishToEmployee(participantNumber, event);
        }
    }
}
