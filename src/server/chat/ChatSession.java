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

public class ChatSession {

    private final String sessionId;

    private final Employee initiator;

    private final Employee partner;

    private final LocalDateTime openedAt;

    private final List<ChatMessage> messages = new ArrayList<>();

    private final Set<String> joinedManagerNumbers = new LinkedHashSet<>();

    private volatile boolean open = true;

    public ChatSession(String sessionId, Employee initiator, Employee partner) {
        this.sessionId = sessionId;
        this.initiator = initiator;
        this.partner = partner;
        this.openedAt = LocalDateTime.now();
    }

    public void addMessage(ChatMessage message) {
        synchronized (this) {
            messages.add(message);
        }
    }

    public List<ChatMessage> getMessageHistory() {
        synchronized (this) {
            return new ArrayList<>(messages);
        }
    }

    public boolean addManager(String managerEmployeeNumber) {
        synchronized (this) {
            return joinedManagerNumbers.add(managerEmployeeNumber);
        }
    }

    public boolean isParticipant(String employeeNumber) {
        if (initiator.getEmployeeNumber().equals(employeeNumber)
                || partner.getEmployeeNumber().equals(employeeNumber)) {
            return true;
        }
        synchronized (this) {
            return joinedManagerNumbers.contains(employeeNumber);
        }
    }

    public List<String> getAllParticipantNumbers() {
        List<String> participants = new ArrayList<>();
        participants.add(initiator.getEmployeeNumber());
        participants.add(partner.getEmployeeNumber());
        synchronized (this) {
            participants.addAll(joinedManagerNumbers);
        }
        return participants;
    }

    public String getOtherOwner(String employeeNumber) {
        if (initiator.getEmployeeNumber().equals(employeeNumber)) {
            return partner.getEmployeeNumber();
        }
        if (partner.getEmployeeNumber().equals(employeeNumber)) {
            return initiator.getEmployeeNumber();
        }
        return null;
    }

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

    public String getSessionId() {
        return sessionId;
    }

    public Employee getInitiator() {
        return initiator;
    }

    public Employee getPartner() {
        return partner;
    }

    public String describeBranches() {
        Branch initiatorBranch = initiator.getBranch();
        Branch partnerBranch = partner.getBranch();
        return initiatorBranch.getDisplayName() + " - " + partnerBranch.getDisplayName();
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.open = false;
    }

    @Override
    public String toString() {
        return "ChatSession " + sessionId + ": " + toSessionInfo().describeParticipants();
    }
}
