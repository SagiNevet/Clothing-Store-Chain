package common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when a password does not satisfy the password policy defined by the
 * administrator.
 * <p>
 * The exception carries the list of every rule that was violated, so the GUI
 * can show the user all the problems at once instead of one at a time.
 * </p>
 */
public class InvalidPasswordPolicyException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** A human readable description of every rule the password violated. */
    private final List<String> violations;

    /**
     * Creates a password policy failure.
     *
     * @param violations the descriptions of the rules that were violated,
     *                   must not be {@code null} or empty
     */
    public InvalidPasswordPolicyException(List<String> violations) {
        super("The password does not meet the password policy: " + String.join("; ", violations));
        this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
    }

    /**
     * Returns the rules that the password violated.
     *
     * @return an unmodifiable list of violation descriptions, never empty
     */
    public List<String> getViolations() {
        return violations;
    }
}
