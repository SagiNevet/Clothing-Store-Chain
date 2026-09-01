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

public final class ChatService {

    private static final ChatService INSTANCE = new ChatService();

    private final Map<String, ChatSession> openSessions = new ConcurrentHashMap<>();

    private ChatService() {
    }

    public static ChatService getInstance() {
        return INSTANCE;
    }

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

        EventPublisher.getInstance().publishToEmployee(partner.getEmployeeNumber(),
                new ServerEvent(EventType.CHAT_INVITE)
                        .withPayload(ProtocolKeys.CHAT_SESSION_ID, session.getSessionId())
                        .withPayload(ProtocolKeys.CHAT_SESSION, session.toSessionInfo()));

        return session;
    }

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

        LogManager.getInstance().logChatAction(sender.getEmployeeNumber(), sender.getBranch(),
                "CHAT_MESSAGE", session.toSessionInfo().describeParticipants(), text);

        deliverToParticipants(session, new ServerEvent(EventType.CHAT_MESSAGE)
                .withPayload(ProtocolKeys.CHAT_SESSION_ID, sessionId)
                .withPayload(ProtocolKeys.CHAT_MESSAGE, message));

        return message;
    }

    public List<ChatMessage> joinChat(Employee manager, String sessionId)
            throws EntityNotFoundException, PermissionDeniedException {

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

        releaseOwner(session.getInitiator());
        releaseOwner(session.getPartner());
    }

    public void releaseOwner(Employee employee) {
        ChatQueueManager queueManager = ChatQueueManager.getInstance();
        queueManager.markFree(employee.getEmployeeNumber());

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

    public List<ChatSessionInfo> getOpenSessions() {
        List<ChatSessionInfo> descriptions = new ArrayList<>();
        for (ChatSession session : openSessions.values()) {
            descriptions.add(session.toSessionInfo());
        }
        return descriptions;
    }

    private ChatSession requireSession(String sessionId) throws EntityNotFoundException {
        ChatSession session = openSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            throw new EntityNotFoundException("Chat session", String.valueOf(sessionId));
        }
        return session;
    }

    private void deliverToParticipants(ChatSession session, ServerEvent event) {
        EventPublisher publisher = EventPublisher.getInstance();
        for (String participantNumber : session.getAllParticipantNumbers()) {
            publisher.publishToEmployee(participantNumber, event);
        }
    }
}
