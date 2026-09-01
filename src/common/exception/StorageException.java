package common.exception;

public class StorageException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final String filePath;

    public StorageException(String filePath, String message, Throwable cause) {
        super(message + " (file: " + filePath + ")", cause);
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
