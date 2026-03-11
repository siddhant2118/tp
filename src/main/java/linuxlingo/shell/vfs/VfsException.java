package linuxlingo.shell.vfs;

/**
 * Runtime exception for VFS operations (path not found, permission denied, etc.).
 */
public class VfsException extends RuntimeException {
    public VfsException(String message) {
        super(message);
    }
}
