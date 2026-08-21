package common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Turns a password into a hash that can be stored safely in the employees file.
 * <p>
 * Passwords are never written to disk as clear text. Instead the system stores
 * two strings for every employee: a random <b>salt</b>, and the SHA-256
 * <b>hash</b> of the salt joined with the password. Verifying a login means
 * hashing the typed password with the stored salt and comparing the results -
 * the original password can never be recovered from the file.
 * </p>
 * <p>
 * <b>Why a salt is needed:</b> without it, two employees who happen to choose
 * the same password would have exactly the same hash in the file, which would
 * be visible at a glance. A random salt per employee makes identical passwords
 * produce completely different hashes.
 * </p>
 * <p>
 * Everything here comes from the standard JDK: {@link MessageDigest},
 * {@link SecureRandom} and {@link Base64}. No external library is used.
 * </p>
 */
public final class PasswordHasher {

    /** The hashing algorithm, available in every standard JDK. */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** The number of random bytes in a salt. */
    private static final int SALT_LENGTH_IN_BYTES = 16;

    /** The random generator used to produce salts. Thread safe by contract. */
    private static final SecureRandom RANDOM_GENERATOR = new SecureRandom();

    /**
     * Prevents instantiation. This class only exposes static utility methods.
     */
    private PasswordHasher() {
    }

    /**
     * Produces a fresh random salt for a new employee account.
     *
     * @return a Base64 text holding {@value #SALT_LENGTH_IN_BYTES} random bytes
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH_IN_BYTES];
        RANDOM_GENERATOR.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Hashes a password together with a salt.
     *
     * @param plainPassword the password typed by the user, must not be {@code null}
     * @param salt          the salt stored for that employee, must not be {@code null}
     * @return the Base64 text of the SHA-256 hash
     * @throws IllegalStateException if the JDK does not provide SHA-256, which
     *                               cannot happen on a standard installation
     */
    public static String hash(String plainPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String saltedPassword = salt + plainPassword;
            byte[] hashedBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException algorithmMissing) {
            // SHA-256 is part of every standard JDK. If it is really missing the
            // program cannot authenticate anybody, so this is a fatal state and
            // not a business failure the user could fix.
            throw new IllegalStateException(
                    "The " + HASH_ALGORITHM + " algorithm is missing from this JDK",
                    algorithmMissing);
        }
    }

    /**
     * Checks a typed password against the values stored for an employee.
     *
     * @param plainPassword the password typed by the user
     * @param salt          the salt stored for that employee
     * @param expectedHash  the hash stored for that employee
     * @return {@code true} if the typed password matches the stored hash
     */
    public static boolean matches(String plainPassword, String salt, String expectedHash) {
        String actualHash = hash(plainPassword, salt);
        return actualHash.equals(expectedHash);
    }
}
