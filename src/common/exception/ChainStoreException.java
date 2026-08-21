package common.exception;

/**
 * The base class of every business exception thrown by this system.
 * <p>
 * It extends {@link Exception} and not {@link RuntimeException} on purpose:
 * these are <b>checked</b> exceptions, which forces the calling code to declare
 * them with {@code throws} and to handle them. A failure such as "not enough
 * items in stock" is an expected business situation, not a programming bug, so
 * the compiler should make sure nobody ignores it.
 * </p>
 * <p>
 * Having one common base class lets a caller either catch a specific failure
 * and react to it, or catch {@code ChainStoreException} once and report any
 * business failure to the user in a single place.
 * </p>
 */
public class ChainStoreException extends Exception {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a business exception with a message meant to be shown to the user.
     *
     * @param message a human readable description of the failure
     */
    public ChainStoreException(String message) {
        super(message);
    }

    /**
     * Creates a business exception that wraps a lower level failure.
     *
     * @param message a human readable description of the failure
     * @param cause   the original exception that caused this failure
     */
    public ChainStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
