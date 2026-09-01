package test;

import common.exception.InvalidPasswordPolicyException;
import common.model.PasswordPolicy;
import common.util.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordPolicyTest {

    private final PasswordPolicy defaultPolicy = PasswordPolicy.createDefault();

    @Test
    @DisplayName("A password that satisfies every rule is accepted")
    public void validPasswordIsAccepted() {
        assertDoesNotThrow(() -> defaultPolicy.validate("Shirt2026"));
    }

    @Test
    @DisplayName("A password that is too short is refused")
    public void shortPasswordIsRefused() {
        InvalidPasswordPolicyException failure = assertThrows(
                InvalidPasswordPolicyException.class,
                () -> defaultPolicy.validate("Ab1"));

        assertEquals(1, failure.getViolations().size());
        assertTrue(failure.getViolations().get(0).contains("at least 8"));
    }

    @Test
    @DisplayName("A password without a digit is refused")
    public void passwordWithoutDigitIsRefused() {
        InvalidPasswordPolicyException failure = assertThrows(
                InvalidPasswordPolicyException.class,
                () -> defaultPolicy.validate("ShirtShirt"));

        assertEquals(1, failure.getViolations().size());
        assertTrue(failure.getViolations().get(0).contains("digit"));
    }

    @Test
    @DisplayName("Every violation is reported at once, not one at a time")
    public void allViolationsAreReportedTogether() {

        InvalidPasswordPolicyException failure = assertThrows(
                InvalidPasswordPolicyException.class,
                () -> defaultPolicy.validate("abc"));

        assertEquals(3, failure.getViolations().size());
    }

    @Test
    @DisplayName("An empty password and a null password are both refused")
    public void emptyAndNullPasswordsAreRefused() {
        assertThrows(InvalidPasswordPolicyException.class, () -> defaultPolicy.validate(""));
        assertThrows(InvalidPasswordPolicyException.class, () -> defaultPolicy.validate(null));
    }

    @Test
    @DisplayName("A stricter policy also demands a special character")
    public void strictPolicyDemandsSpecialCharacter() {
        PasswordPolicy strictPolicy = new PasswordPolicy(10, true, true, true, true);

        assertThrows(InvalidPasswordPolicyException.class,
                () -> strictPolicy.validate("ShirtShirt1"));
        assertDoesNotThrow(() -> strictPolicy.validate("ShirtShirt1!"));
    }

    @Test
    @DisplayName("A minimum length below four is refused when the policy is built")
    public void unreasonableMinimumLengthIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordPolicy(2, false, false, false, false));
    }

    @Test
    @DisplayName("The same password produces a different hash for every employee")
    public void identicalPasswordsProduceDifferentHashes() {
        String sharedPassword = "Shirt2026";
        String saltOfFirstEmployee = PasswordHasher.generateSalt();
        String saltOfSecondEmployee = PasswordHasher.generateSalt();

        String hashOfFirstEmployee = PasswordHasher.hash(sharedPassword, saltOfFirstEmployee);
        String hashOfSecondEmployee = PasswordHasher.hash(sharedPassword, saltOfSecondEmployee);

        assertNotEquals(saltOfFirstEmployee, saltOfSecondEmployee);
        assertNotEquals(hashOfFirstEmployee, hashOfSecondEmployee);
    }

    @Test
    @DisplayName("A password matches its own hash and nothing else")
    public void passwordMatchesOnlyItsOwnHash() {
        String salt = PasswordHasher.generateSalt();
        String storedHash = PasswordHasher.hash("Shirt2026", salt);

        assertTrue(PasswordHasher.matches("Shirt2026", salt, storedHash));
        assertFalse(PasswordHasher.matches("shirt2026", salt, storedHash));
        assertFalse(PasswordHasher.matches("Shirt2027", salt, storedHash));
    }

    @Test
    @DisplayName("The stored hash is not the password itself")
    public void storedHashDoesNotContainThePassword() {
        String salt = PasswordHasher.generateSalt();
        String storedHash = PasswordHasher.hash("Shirt2026", salt);

        assertFalse(storedHash.contains("Shirt2026"));
    }
}
