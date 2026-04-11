package linuxlingo.shell.vfs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a directory node in the virtual file system.
 *
 * <p>A directory maintains an insertion-ordered map of child nodes,
 * allowing traversal and manipulation of the VFS tree.</p>
 */
public class Directory extends FileNode {
    /** Ordered mapping from child name to child node. */
    private final LinkedHashMap<String, FileNode> children;

    /**
     * Constructs an empty directory with the given name and permission.
     *
     * @param name the directory name.
     * @param perm the permission set for this directory.
     */
    public Directory(String name, Permission perm) {
        super(name, perm);
        this.children = new LinkedHashMap<>();
    }

    /**
     * Adds a child node to this directory, setting its parent reference.
     *
     * @param child the node to add.
     */
    public void addChild(FileNode child) {
        if (child == null) {
            throw new IllegalArgumentException("child must not be null");
        }
        VirtualFileSystem.validateFileName(child.getName());
        child.setParent(this);
        children.put(child.getName(), child);
    }

    /**
     * Removes the child with the given name from this directory.
     * If the child exists, its parent reference is cleared.
     *
     * @param name the name of the child to remove.
     */
    public void removeChild(String name) {
        FileNode child = children.remove(name);
        if (child != null) {
            child.setParent(null);
        }
    }

    /**
     * Returns the child node with the given name, or {@code null} if not found.
     *
     * @param name the name of the child to look up.
     * @return the child node, or {@code null}.
     */
    public FileNode getChild(String name) {
        return children.get(name);
    }

    /**
     * Returns a list of all child nodes in insertion order.
     *
     * @return a new list containing all children.
     */
    public List<FileNode> getChildren() {
        return new ArrayList<>(children.values());
    }

    /**
     * Checks whether this directory contains a child with the given name.
     *
     * @param name the child name to check.
     * @return {@code true} if a child with this name exists.
     */
    public boolean hasChild(String name) {
        return children.containsKey(name);
    }

    /**
     * Checks whether this directory has no children.
     *
     * @return {@code true} if the directory is empty.
     */
    public boolean isEmpty() {
        return children.isEmpty();
    }

    /**
     * {@inheritDoc}
     * Always returns {@code true} for directories.
     */
    @Override
    public boolean isDirectory() {
        return true;
    }

    /**
     * {@inheritDoc}
     * Creates a deep copy of this directory and all its descendants.
     */
    @Override
    public Directory deepCopy() {
        Directory copy = new Directory(getName(), getPermission().copy());
        for (Map.Entry<String, FileNode> entry : children.entrySet()) {
            FileNode childCopy = entry.getValue().deepCopy();
            copy.addChild(childCopy);
        }
        return copy;
    }
}
