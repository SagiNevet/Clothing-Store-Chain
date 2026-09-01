package common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final int SALT_LENGTH_IN_BYTES = 16;

    private static final SecureRandom RANDOM_GENERATOR = new SecureRandom();

    private PasswordHasher() {
    }

    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH_IN_BYTES];
        RANDOM_GENERATOR.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String hash(String plainPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String saltedPassword = salt + plainPassword;
            byte[] hashedBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException algorithmMissing) {

            throw new IllegalStateException(
                    "The " + HASH_ALGORITHM + " algorithm is missing from this JDK",
                    algorithmMissing);
        }
    }

    public static boolean matches(String plainPassword, String salt, String expectedHash) {
        String actualHash = hash(plainPassword, salt);
        return actualHash.equals(expectedHash);
    }
}
