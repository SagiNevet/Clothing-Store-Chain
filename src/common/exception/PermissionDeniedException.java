package common.exception;

import common.model.Role;

/**
 * Thrown when an employee asks the server to perform an action that the
 * employee role does not allow, for example a cashier trying to join an
 * existing chat session or to open the reports screen.
 * <p>
 * The check is performed on the <b>server</b> and not only by hiding buttons in
 * the GUI. Hiding a button is a convenience for the user; it is not security,
 * because a modified client could still send the request.
 * </p>
 */
public class PermissionDeniedException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** The role that attempted the action. */
    private final Role role;

    /** A short description of the action that was refused. */
    private final String attemptedAction;

    /**
     * Creates a permission failure.
     *
     * @param role            the role of the employee who attempted the action
     * @param attemptedAction a short description of the refused action
     */
    public PermissionDeniedException(Role role, String attemptedAction) {
        super("Role " + role.getDisplayName() + " is not allowed to perform: " + attemptedAction);
        this.role = role;
        this.attemptedAction = attemptedAction;
    }

    /**
     * Returns the role that attempted the action.
     *
     * @return the role of the employee
     */
    public Role getRole() {
        return role;
    }

    /**
     * Returns the action that was refused.
     *
     * @return a short description of the attempted action
     */
    public String getAttemptedAction() {
        return attemptedAction;
    }
}
