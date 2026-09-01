package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sessionId;

    private final String senderEmployeeNumber;

    private final String senderFullName;

    private final Branch senderBranch;

    private final String content;

    private final LocalDateTime sentAt;

    public ChatMessage(String sessionId, String senderEmployeeNumber, String senderFullName,
                       Branch senderBranch, String content) {
        this.sessionId = sessionId;
        this.senderEmployeeNumber = senderEmployeeNumber;
        this.senderFullName = senderFullName;
        this.senderBranch = senderBranch;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSenderEmployeeNumber() {
        return senderEmployeeNumber;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public Branch getSenderBranch() {
        return senderBranch;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public String toDisplayLine() {
        return "[" + TimeUtil.formatForDisplay(sentAt) + "] "
                + senderFullName + " (" + senderBranch.getDisplayName() + "): " + content;
    }

    @Override
    public String toString() {
        return toDisplayLine();
    }
}
