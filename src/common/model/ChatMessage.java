package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One message inside a chat session between two branches.
 * <p>
 * The message carries the branch of the sender and not only the name, because
 * the whole point of the chat feature is a conversation between branches, and a
 * shift manager who joins an existing session needs to see immediately who is
 * speaking from where.
 * </p>
 */
public class ChatMessage implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The session this message belongs to. */
    private final String sessionId;

    /** The employee number of the sender. */
    private final String senderEmployeeNumber;

    /** The full name of the sender, copied so the client does not have to look it up. */
    private final String senderFullName;

    /** The branch the sender is sitting in. */
    private final Branch senderBranch;

    /** The text of the message. */
    private final String content;

    /** The moment the message was sent. */
    private final LocalDateTime sentAt;

    /**
     * Creates a chat message.
     *
     * @param sessionId            the session this message belongs to
     * @param senderEmployeeNumber the employee number of the sender
     * @param senderFullName       the full name of the sender
     * @param senderBranch         the branch the sender is sitting in
     * @param content              the text of the message
     */
    public ChatMessage(String sessionId, String senderEmployeeNumber, String senderFullName,
                       Branch senderBranch, String content) {
        this.sessionId = sessionId;
        this.senderEmployeeNumber = senderEmployeeNumber;
        this.senderFullName = senderFullName;
        this.senderBranch = senderBranch;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Returns the session this message belongs to.
     *
     * @return the chat session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the employee number of the sender.
     *
     * @return the sender employee number
     */
    public String getSenderEmployeeNumber() {
        return senderEmployeeNumber;
    }

    /**
     * Returns the full name of the sender.
     *
     * @return the sender full name
     */
    public String getSenderFullName() {
        return senderFullName;
    }

    /**
     * Returns the branch the sender is sitting in.
     *
     * @return the sender branch
     */
    public Branch getSenderBranch() {
        return senderBranch;
    }

    /**
     * Returns the text of the message.
     *
     * @return the message content
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the moment the message was sent.
     *
     * @return the time the message was sent
     */
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    /**
     * Builds the line displayed in the chat window.
     *
     * @return the message formatted as {@code [time] name (branch): text}
     */
    public String toDisplayLine() {
        return "[" + TimeUtil.formatForDisplay(sentAt) + "] "
                + senderFullName + " (" + senderBranch.getDisplayName() + "): " + content;
    }

    @Override
    public String toString() {
        return toDisplayLine();
    }
}
