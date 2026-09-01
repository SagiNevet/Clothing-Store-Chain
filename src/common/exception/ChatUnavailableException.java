package common.exception;

public class ChatUnavailableException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final boolean queuedForCallback;

    public ChatUnavailableException(String message, boolean queuedForCallback) {
        super(message);
        this.queuedForCallback = queuedForCallback;
    }

    public boolean isQueuedForCallback() {
        return queuedForCallback;
    }
}
