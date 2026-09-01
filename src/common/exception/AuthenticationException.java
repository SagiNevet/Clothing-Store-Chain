package common.exception;

public class AuthenticationException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private static final String GENERIC_FAILURE_MESSAGE = "Invalid employee number or password";

    public AuthenticationException() {
        super(GENERIC_FAILURE_MESSAGE);
    }
}
