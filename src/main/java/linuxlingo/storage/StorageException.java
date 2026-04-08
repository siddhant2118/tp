package linuxlingo.storage;

/**
 * Represents a checked exception thrown when a storage I/O operation fails.
 *
 * <p>Used by {@link Storage}, {@code VfsSerializer}, and other components
 * that perform disk I/O to signal recoverable errors.</p>
 */
public class StorageException extends Exception {

    /**
     * Constructs a StorageException with the specified detail message.
     *
     * @param message the detail message.
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Constructs a StorageException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the underlying cause of this exception.
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
