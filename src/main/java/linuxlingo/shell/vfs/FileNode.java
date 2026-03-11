package linuxlingo.shell.vfs;

/**
 * Abstract base class for all VFS nodes (files and directories).
 */
public abstract class FileNode {
    private String name;
    private Permission permission;
    private Directory parent;

    protected FileNode(String name, Permission permission) {
        this.name = name;
        this.permission = permission;
        this.parent = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public Directory getParent() {
        return parent;
    }

    public void setParent(Directory parent) {
        this.parent = parent;
    }

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

    public abstract boolean isDirectory();

    public abstract FileNode deepCopy();
}
