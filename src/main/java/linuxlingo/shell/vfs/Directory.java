package linuxlingo.shell.vfs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A directory node in the VFS. Contains child nodes.
 */
public class Directory extends FileNode {
    private final LinkedHashMap<String, FileNode> children;

    public Directory(String name, Permission perm) {
        super(name, perm);
        this.children = new LinkedHashMap<>();
    }

    public void addChild(FileNode child) {
        child.setParent(this);
        children.put(child.getName(), child);
    }

    public void removeChild(String name) {
        FileNode child = children.remove(name);
        if (child != null) {
            child.setParent(null);
        }
    }

    public FileNode getChild(String name) {
        return children.get(name);
    }

    public List<FileNode> getChildren() {
        return new ArrayList<>(children.values());
    }

    public boolean hasChild(String name) {
        return children.containsKey(name);
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

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
