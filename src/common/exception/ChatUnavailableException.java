package common.exception;

/**
 * Thrown when a chat request cannot be served right now, for example when no
 * employee in the target branch is free, or when the same employee tries to
 * open a second chat from another computer.
 * <p>
 * This exception does not mean the request was lost. When no employee is
 * available the server also stores the request in the waiting queue, and the
 * requester will receive a notification once somebody becomes free.
 * </p>
 */
public class ChatUnavailableException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** Whether the request was placed in the waiting queue for a later callback. */
    private final boolean queuedForCallback;

    /**
     * Creates a chat availability failure.
     *
     * @param message           a human readable description of the failure
     * @param queuedForCallback {@code true} if the request was stored in the
     *                          waiting queue and the requester will be notified later
     */
    public ChatUnavailableException(String message, boolean queuedForCallback) {
        super(message);
        this.queuedForCallback = queuedForCallback;
    }

    /**
     * Indicates whether the request is waiting in the queue for a callback.
     *
     * @return {@code true} if the requester will be notified when somebody
     *         becomes available, {@code false} if the request was dropped
     */
    public boolean isQueuedForCallback() {
        return queuedForCallback;
    }
}
