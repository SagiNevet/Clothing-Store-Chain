package common.exception;

/**
 * Thrown when reading from or writing to one of the data files fails.
 * <p>
 * This class wraps the low level {@link java.io.IOException} and
 * {@link ClassNotFoundException} thrown by the object streams, so that the
 * business layer above the storage layer never has to know that the data
 * happens to be kept in files. If the storage were replaced tomorrow, only this
 * package would change.
 * </p>
 */
public class StorageException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** The path of the file that could not be read or written. */
    private final String filePath;

    /**
     * Creates a storage failure that wraps the original I/O exception.
     *
     * @param filePath the path of the file that could not be read or written
     * @param message  a human readable description of the failure
     * @param cause    the original exception thrown by the streams
     */
    public StorageException(String filePath, String message, Throwable cause) {
        super(message + " (file: " + filePath + ")", cause);
        this.filePath = filePath;
    }

    /**
     * Returns the file that caused the failure.
     *
     * @return the path of the problematic file
     */
    public String getFilePath() {
        return filePath;
    }
}
