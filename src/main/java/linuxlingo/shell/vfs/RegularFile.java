package linuxlingo.shell.vfs;

/**
 * A regular file node in the VFS. Holds text content.
 */
public class RegularFile extends FileNode {
    private String content;

    public RegularFile(String name, Permission perm, String content) {
        super(name, perm);
        this.content = content == null ? "" : content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public void appendContent(String text) {
        this.content += text;
    }

    public int getSize() {
        return content.length();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public RegularFile deepCopy() {
        return new RegularFile(getName(), getPermission().copy(), content);
    }
}
