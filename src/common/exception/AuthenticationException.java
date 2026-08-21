package common.exception;

/**
 * Thrown when a login attempt fails because the employee number is unknown or
 * the password is wrong.
 * <p>
 * Both cases produce the <b>same</b> message on purpose. Telling the user
 * "this employee number does not exist" would let an attacker discover which
 * employee numbers are valid, so the server answers with one generic sentence.
 * </p>
 */
public class AuthenticationException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** The single message returned for every kind of failed login. */
    private static final String GENERIC_FAILURE_MESSAGE = "Invalid employee number or password";

    /**
     * Creates an authentication failure carrying the generic message.
     */
    public AuthenticationException() {
        super(GENERIC_FAILURE_MESSAGE);
    }
}
