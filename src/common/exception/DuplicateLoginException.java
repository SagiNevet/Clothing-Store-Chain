package common.exception;

/**
 * Thrown when an employee who already has an open session tries to log in from
 * a second computer.
 * <p>
 * The requirement is that the same user is never connected twice at the same
 * time. The server keeps a table of active sessions and throws this exception
 * when a second login arrives for a user that is already in that table.
 * </p>
 */
public class DuplicateLoginException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** The employee number that is already connected. */
    private final String employeeNumber;

    /**
     * Creates a duplicate login failure.
     *
     * @param employeeNumber the employee number that is already connected
     */
    public DuplicateLoginException(String employeeNumber) {
        super("Employee " + employeeNumber + " is already logged in from another computer");
        this.employeeNumber = employeeNumber;
    }

    /**
     * Returns the employee number that is already connected.
     *
     * @return the employee number
     */
    public String getEmployeeNumber() {
        return employeeNumber;
    }
}
