package linuxlingo.shell.vfs;

/**
 * Represents a regular (non-directory) file node in the virtual file system.
 *
 * <p>Holds text content as a {@link String}. Null content is normalised
 * to an empty string on construction and mutation.</p>
 */
public class RegularFile extends FileNode {
    /** Text content stored in this file. Never {@code null}. */
    private String content;

    /**
     * Constructs a regular file with the given name, permission, and content.
     *
     * @param name    the file name.
     * @param perm    the permission set for this file.
     * @param content the initial text content, or {@code null} for empty.
     */
    public RegularFile(String name, Permission perm, String content) {
        super(name, perm);
        this.content = content == null ? "" : content;
    }

    /**
     * Returns the text content of this file.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the text content of this file.
     * A {@code null} value is normalised to an empty string.
     *
     * @param content the new content.
     */
    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    /**
     * Appends text to the existing content of this file.
     *
     * @param text the text to append.
     */
    public void appendContent(String text) {
        this.content += text;
    }

    /**
     * Returns the size of this file in characters.
     */
    public int getSize() {
        return content.length();
    }

    /**
     * {@inheritDoc}
     * Always returns {@code false} for regular files.
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Creates a deep copy of this file, including its content and permission.
     */
    @Override
    public RegularFile deepCopy() {
        return new RegularFile(getName(), getPermission().copy(), content);
    }
}
