package common.model;

import common.exception.InvalidPasswordPolicyException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The password rules defined by the administrator, and the code that enforces
 * them.
 * <p>
 * The policy is stored in its own file on the server, so a change made by the
 * administrator survives a restart and applies to every branch.
 * </p>
 * <p>
 * <b>Exception handling note:</b> {@link #validate(String)} collects <b>all</b>
 * the violations and then throws one
 * {@link InvalidPasswordPolicyException} carrying the complete list. The
 * alternative - returning {@code false}, or throwing on the first problem -
 * would force the calling screen to test the password again and again to
 * discover every mistake, and would mix validation code into the calling logic.
 * Here the caller has one {@code try} block with the business flow inside it and
 * one {@code catch} block that only displays the collected violations.
 * </p>
 */
public class PasswordPolicy implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The minimum length used when no policy file exists yet. */
    private static final int DEFAULT_MINIMUM_LENGTH = 8;

    /** The characters treated as special characters by the policy. */
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{};:,.<>?/";

    /** The smallest minimum length an administrator may configure. */
    private static final int SMALLEST_ALLOWED_MINIMUM_LENGTH = 4;

    /** The smallest number of characters a password must contain. */
    private int minimumLength;

    /** Whether a password must contain at least one digit. */
    private boolean digitRequired;

    /** Whether a password must contain at least one capital letter. */
    private boolean upperCaseLetterRequired;

    /** Whether a password must contain at least one small letter. */
    private boolean lowerCaseLetterRequired;

    /** Whether a password must contain at least one special character. */
    private boolean specialCharacterRequired;

    /**
     * Creates a password policy.
     *
     * @param minimumLength            the smallest number of characters allowed
     * @param digitRequired            whether a digit is required
     * @param upperCaseLetterRequired  whether a capital letter is required
     * @param lowerCaseLetterRequired  whether a small letter is required
     * @param specialCharacterRequired whether a special character is required
     * @throws IllegalArgumentException if the minimum length is below
     *                                  {@value #SMALLEST_ALLOWED_MINIMUM_LENGTH}
     */
    public PasswordPolicy(int minimumLength, boolean digitRequired,
                          boolean upperCaseLetterRequired, boolean lowerCaseLetterRequired,
                          boolean specialCharacterRequired) {
        if (minimumLength < SMALLEST_ALLOWED_MINIMUM_LENGTH) {
            throw new IllegalArgumentException("minimumLength must be at least "
                    + SMALLEST_ALLOWED_MINIMUM_LENGTH + ", received " + minimumLength);
        }
        this.minimumLength = minimumLength;
        this.digitRequired = digitRequired;
        this.upperCaseLetterRequired = upperCaseLetterRequired;
        this.lowerCaseLetterRequired = lowerCaseLetterRequired;
        this.specialCharacterRequired = specialCharacterRequired;
    }

    /**
     * Creates the policy used the first time the server starts, before an
     * administrator has configured anything: at least
     * {@value #DEFAULT_MINIMUM_LENGTH} characters, with a digit, a capital
     * letter and a small letter, and without demanding a special character.
     *
     * @return a reasonable default policy
     */
    public static PasswordPolicy createDefault() {
        return new PasswordPolicy(DEFAULT_MINIMUM_LENGTH, true, true, true, false);
    }

    /**
     * Checks a password against every rule of this policy.
     *
     * @param plainPassword the password the user typed
     * @throws InvalidPasswordPolicyException if the password breaks at least one
     *                                        rule; the exception carries the
     *                                        complete list of violations
     */
    public void validate(String plainPassword) throws InvalidPasswordPolicyException {
        List<String> violations = new ArrayList<>();
        String passwordToCheck = plainPassword == null ? "" : plainPassword;

        if (passwordToCheck.length() < minimumLength) {
            violations.add("must contain at least " + minimumLength + " characters");
        }
        if (digitRequired && !containsDigit(passwordToCheck)) {
            violations.add("must contain at least one digit");
        }
        if (upperCaseLetterRequired && !containsUpperCaseLetter(passwordToCheck)) {
            violations.add("must contain at least one capital letter");
        }
        if (lowerCaseLetterRequired && !containsLowerCaseLetter(passwordToCheck)) {
            violations.add("must contain at least one small letter");
        }
        if (specialCharacterRequired && !containsSpecialCharacter(passwordToCheck)) {
            violations.add("must contain at least one special character such as " + SPECIAL_CHARACTERS);
        }

        if (!violations.isEmpty()) {
            throw new InvalidPasswordPolicyException(violations);
        }
    }

    /**
     * Checks whether a password contains a digit.
     *
     * @param password the password to scan
     * @return {@code true} if at least one character is a digit
     */
    private boolean containsDigit(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (Character.isDigit(currentCharacter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a password contains a capital letter.
     *
     * @param password the password to scan
     * @return {@code true} if at least one character is a capital letter
     */
    private boolean containsUpperCaseLetter(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (Character.isUpperCase(currentCharacter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a password contains a small letter.
     *
     * @param password the password to scan
     * @return {@code true} if at least one character is a small letter
     */
    private boolean containsLowerCaseLetter(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (Character.isLowerCase(currentCharacter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a password contains one of the special characters.
     *
     * @param password the password to scan
     * @return {@code true} if at least one character appears in
     *         {@link #SPECIAL_CHARACTERS}
     */
    private boolean containsSpecialCharacter(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (SPECIAL_CHARACTERS.indexOf(currentCharacter) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a sentence describing the policy, to be displayed next to the
     * password field.
     *
     * @return a human readable description of the rules
     */
    public String describe() {
        StringBuilder description = new StringBuilder();
        description.append("At least ").append(minimumLength).append(" characters");
        if (digitRequired) {
            description.append(", one digit");
        }
        if (upperCaseLetterRequired) {
            description.append(", one capital letter");
        }
        if (lowerCaseLetterRequired) {
            description.append(", one small letter");
        }
        if (specialCharacterRequired) {
            description.append(", one special character");
        }
        return description.toString();
    }

    /**
     * Returns the smallest number of characters a password must contain.
     *
     * @return the minimum length
     */
    public int getMinimumLength() {
        return minimumLength;
    }

    /**
     * Updates the smallest number of characters a password must contain.
     *
     * @param minimumLength the new minimum length
     * @throws IllegalArgumentException if the value is below
     *                                  {@value #SMALLEST_ALLOWED_MINIMUM_LENGTH}
     */
    public void setMinimumLength(int minimumLength) {
        if (minimumLength < SMALLEST_ALLOWED_MINIMUM_LENGTH) {
            throw new IllegalArgumentException("minimumLength must be at least "
                    + SMALLEST_ALLOWED_MINIMUM_LENGTH + ", received " + minimumLength);
        }
        this.minimumLength = minimumLength;
    }

    /**
     * Indicates whether a digit is required.
     *
     * @return {@code true} if a password must contain a digit
     */
    public boolean isDigitRequired() {
        return digitRequired;
    }

    /**
     * Sets whether a digit is required.
     *
     * @param digitRequired {@code true} to require a digit
     */
    public void setDigitRequired(boolean digitRequired) {
        this.digitRequired = digitRequired;
    }

    /**
     * Indicates whether a capital letter is required.
     *
     * @return {@code true} if a password must contain a capital letter
     */
    public boolean isUpperCaseLetterRequired() {
        return upperCaseLetterRequired;
    }

    /**
     * Sets whether a capital letter is required.
     *
     * @param upperCaseLetterRequired {@code true} to require a capital letter
     */
    public void setUpperCaseLetterRequired(boolean upperCaseLetterRequired) {
        this.upperCaseLetterRequired = upperCaseLetterRequired;
    }

    /**
     * Indicates whether a small letter is required.
     *
     * @return {@code true} if a password must contain a small letter
     */
    public boolean isLowerCaseLetterRequired() {
        return lowerCaseLetterRequired;
    }

    /**
     * Sets whether a small letter is required.
     *
     * @param lowerCaseLetterRequired {@code true} to require a small letter
     */
    public void setLowerCaseLetterRequired(boolean lowerCaseLetterRequired) {
        this.lowerCaseLetterRequired = lowerCaseLetterRequired;
    }

    /**
     * Indicates whether a special character is required.
     *
     * @return {@code true} if a password must contain a special character
     */
    public boolean isSpecialCharacterRequired() {
        return specialCharacterRequired;
    }

    /**
     * Sets whether a special character is required.
     *
     * @param specialCharacterRequired {@code true} to require a special character
     */
    public void setSpecialCharacterRequired(boolean specialCharacterRequired) {
        this.specialCharacterRequired = specialCharacterRequired;
    }

    @Override
    public String toString() {
        return "PasswordPolicy [" + describe() + "]";
    }
}
