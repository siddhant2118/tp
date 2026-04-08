package linuxlingo.shell.vfs;

/**
 * Represents a runtime exception thrown by VFS operations.
 *
 * <p>Typical causes include path not found, permission denied,
 * or invalid file-system operations such as writing to a directory.</p>
 */
public class VfsException extends RuntimeException {

    /**
     * Constructs a VFS exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public VfsException(String message) {
        super(message);
    }
}
