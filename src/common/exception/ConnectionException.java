package common.exception;

/**
 * Thrown when the client cannot talk to the server: the connection could not be
 * opened, it dropped in the middle, or an answer never arrived.
 * <p>
 * This failure is deliberately separated from the business failures. A business
 * failure such as a wrong password means the request was understood and
 * refused, and the user should correct the input. A connection failure means
 * nothing was decided at all, and the right reaction is different: check that
 * the server is running and try again.
 * </p>
 */
public class ConnectionException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a connection failure.
     *
     * @param message a human readable description of the failure
     */
    public ConnectionException(String message) {
        super(message);
    }

    /**
     * Creates a connection failure that wraps the original network exception.
     *
     * @param message a human readable description of the failure
     * @param cause   the original exception thrown by the socket or the streams
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
