package common.exception;

public class ChainStoreException extends Exception {

    private static final long serialVersionUID = 1L;

    public ChainStoreException(String message) {
        super(message);
    }

    public ChainStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
