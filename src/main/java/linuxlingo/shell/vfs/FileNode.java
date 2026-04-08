package linuxlingo.shell.vfs;

/**
 * Represents an abstract base class for all VFS nodes (files and directories).
 *
 * <p>Each node has a name, a Unix-like permission set, and an optional parent
 * directory reference that forms the tree structure of the virtual file system.</p>
 */
public abstract class FileNode {
    /** Name of this node within its parent directory. */
    private String name;

    /** Unix-like permission model for this node. */
    private Permission permission;

    /** Parent directory, or {@code null} if this is the root node. */
    private Directory parent;

    /**
     * Constructs a new file node with the given name and permission.
     *
     * @param name       the name of this node.
     * @param permission the permission set for this node.
     */
    protected FileNode(String name, Permission permission) {
        this.name = name;
        this.permission = permission;
        this.parent = null;
    }

    /**
     * Returns the name of this node.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this node.
     *
     * @param name the new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the permission set for this node.
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Sets the permission set for this node.
     *
     * @param permission the new permission.
     */
    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    /**
     * Returns the parent directory of this node, or {@code null} for the root.
     */
    public Directory getParent() {
        return parent;
    }

    /**
     * Sets the parent directory of this node.
     *
     * @param parent the parent directory, or {@code null} to detach.
     */
    public void setParent(Directory parent) {
        this.parent = parent;
    }

    /**
     * Computes and returns the absolute path of this node by traversing
     * parent references up to the root.
     *
     * @return the absolute path string (e.g. {@code "/home/user/file.txt"}).
     */
    public String getAbsolutePath() {
        if (parent == null) {
            return "/";
        }
        String parentPath = parent.getAbsolutePath();
        if (parentPath.equals("/")) {
            return "/" + name;
        }
        return parentPath + "/" + name;
    }

    /**
     * Checks whether this node is a directory.
     *
     * @return {@code true} if this node is a directory, {@code false} otherwise.
     */
    public abstract boolean isDirectory();

    /**
     * Creates and returns a deep copy of this node, including its permission.
     *
     * @return a new {@code FileNode} that is a deep copy of this instance.
     */
    public abstract FileNode deepCopy();
}
