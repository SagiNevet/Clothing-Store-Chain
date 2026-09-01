package common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InvalidPasswordPolicyException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final List<String> violations;

    public InvalidPasswordPolicyException(List<String> violations) {
        super("The password does not meet the password policy: " + String.join("; ", violations));
        this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
    }

    public List<String> getViolations() {
        return violations;
    }
}
