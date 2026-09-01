package common.model;

import common.exception.InvalidPasswordPolicyException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PasswordPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_MINIMUM_LENGTH = 8;

    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{};:,.<>?/";

    private static final int SMALLEST_ALLOWED_MINIMUM_LENGTH = 4;

    private int minimumLength;

    private boolean digitRequired;

    private boolean upperCaseLetterRequired;

    private boolean lowerCaseLetterRequired;

    private boolean specialCharacterRequired;

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

    public static PasswordPolicy createDefault() {
        return new PasswordPolicy(DEFAULT_MINIMUM_LENGTH, true, true, true, false);
    }

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

    private boolean containsDigit(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (Character.isDigit(currentCharacter)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUpperCaseLetter(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (Character.isUpperCase(currentCharacter)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLowerCaseLetter(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (Character.isLowerCase(currentCharacter)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSpecialCharacter(String password) {
        for (char currentCharacter : password.toCharArray()) {
            if (SPECIAL_CHARACTERS.indexOf(currentCharacter) >= 0) {
                return true;
            }
        }
        return false;
    }

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

    public int getMinimumLength() {
        return minimumLength;
    }

    public void setMinimumLength(int minimumLength) {
        if (minimumLength < SMALLEST_ALLOWED_MINIMUM_LENGTH) {
            throw new IllegalArgumentException("minimumLength must be at least "
                    + SMALLEST_ALLOWED_MINIMUM_LENGTH + ", received " + minimumLength);
        }
        this.minimumLength = minimumLength;
    }

    public boolean isDigitRequired() {
        return digitRequired;
    }

    public void setDigitRequired(boolean digitRequired) {
        this.digitRequired = digitRequired;
    }

    public boolean isUpperCaseLetterRequired() {
        return upperCaseLetterRequired;
    }

    public void setUpperCaseLetterRequired(boolean upperCaseLetterRequired) {
        this.upperCaseLetterRequired = upperCaseLetterRequired;
    }

    public boolean isLowerCaseLetterRequired() {
        return lowerCaseLetterRequired;
    }

    public void setLowerCaseLetterRequired(boolean lowerCaseLetterRequired) {
        this.lowerCaseLetterRequired = lowerCaseLetterRequired;
    }

    public boolean isSpecialCharacterRequired() {
        return specialCharacterRequired;
    }

    public void setSpecialCharacterRequired(boolean specialCharacterRequired) {
        this.specialCharacterRequired = specialCharacterRequired;
    }

    @Override
    public String toString() {
        return "PasswordPolicy [" + describe() + "]";
    }
}
