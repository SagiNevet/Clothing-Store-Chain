package common.exception;

import common.model.Role;

public class PermissionDeniedException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final Role role;

    private final String attemptedAction;

    public PermissionDeniedException(Role role, String attemptedAction) {
        super("Role " + role.getDisplayName() + " is not allowed to perform: " + attemptedAction);
        this.role = role;
        this.attemptedAction = attemptedAction;
    }

    public Role getRole() {
        return role;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}
