package linuxlingo.shell.vfs;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade for Virtual File System operations.
 * Provides path resolution, file/directory creation, deletion, copy, move, etc.
 */
public class VirtualFileSystem {
    private final Directory root;

    public VirtualFileSystem() {
        this.root = createDefaultTree();
    }

    public VirtualFileSystem(Directory root) {
        this.root = root;
    }

    /**
     * Create the default VFS tree structure.
     */
    public static Directory createDefaultTree() {
        Directory root = new Directory("", new Permission("rwxr-xr-x"));
        Directory home = new Directory("home", new Permission("rwxr-xr-x"));
        Directory user = new Directory("user", new Permission("rwxr-xr-x"));
        Directory tmp = new Directory("tmp", new Permission("rwxrwxrwx"));
        Directory etc = new Directory("etc", new Permission("rwxr-xr-x"));
        RegularFile hostname = new RegularFile("hostname", new Permission("rw-r--r--"), "linuxlingo");
        root.addChild(home);
        home.addChild(user);
        root.addChild(tmp);
        root.addChild(etc);
        etc.addChild(hostname);
        return root;
    }

    /**
     * Resolve a path string to a FileNode.
     * @throws VfsException if path not found
     */
    public FileNode resolve(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        FileNode current = root;
        for (String part : parts) {
            if (!current.isDirectory()) {
                throw new VfsException("Not a directory: " + current.getAbsolutePath());
            }
            FileNode child = ((Directory) current).getChild(part);
            if (child == null) {
                throw new VfsException("No such file or directory: " + path);
            }
            current = child;
        }
        return current;
    }

    /**
     * Resolve the parent directory of a path.
     */
    public Directory resolveParent(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        if (parts.isEmpty()) {
            return null; // root has no parent
        }
        FileNode current = root;
        for (int i = 0; i < parts.size() - 1; i++) {
            if (!current.isDirectory()) {
                throw new VfsException("Not a directory: " + current.getAbsolutePath());
            }
            FileNode child = ((Directory) current).getChild(parts.get(i));
            if (child == null) {
                throw new VfsException("No such file or directory: " + path);
            }
            current = child;
        }
        if (!current.isDirectory()) {
            throw new VfsException("Not a directory: " + current.getAbsolutePath());
        }
        return (Directory) current;
    }

    /**
     * Get the basename (last component) of a path.
     */
    public String getBaseName(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        if (parts.isEmpty()) {
            return "";
        }
        return parts.get(parts.size() - 1);
    }

    public RegularFile createFile(String path, String workingDir) {
        Directory parent = resolveParent(path, workingDir);
        if (parent == null) {
            throw new VfsException("Cannot create file at root");
        }
        String name = getBaseName(path, workingDir);
        if (parent.hasChild(name)) {
            FileNode existing = parent.getChild(name);
            if (existing.isDirectory()) {
                throw new VfsException("Is a directory: " + path);
            }
            return (RegularFile) existing; // touch existing file
        }
        if (!parent.getPermission().canOwnerWrite()) {
            throw new VfsException("Permission denied: " + path);
        }
        RegularFile file = new RegularFile(name, new Permission("rw-r--r--"), "");
        parent.addChild(file);
        return file;
    }

    public Directory createDirectory(String path, String workingDir, boolean parents) {
        if (parents) {
            return createDirectoryRecursive(path, workingDir);
        }
        Directory parent = resolveParent(path, workingDir);
        if (parent == null) {
            throw new VfsException("Cannot create directory at root");
        }
        String name = getBaseName(path, workingDir);
        if (parent.hasChild(name)) {
            FileNode existing = parent.getChild(name);
            if (existing.isDirectory()) {
                if (parents) {
                    return (Directory) existing;
                }
                throw new VfsException("Directory already exists: " + path);
            }
            throw new VfsException("File exists: " + path);
        }
        if (!parent.getPermission().canOwnerWrite()) {
            throw new VfsException("Permission denied: " + path);
        }
        Directory dir = new Directory(name, new Permission("rwxr-xr-x"));
        parent.addChild(dir);
        return dir;
    }

    private Directory createDirectoryRecursive(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        FileNode current = root;
        for (String part : parts) {
            if (!current.isDirectory()) {
                throw new VfsException("Not a directory: " + current.getAbsolutePath());
            }
            Directory dir = (Directory) current;
            if (dir.hasChild(part)) {
                FileNode child = dir.getChild(part);
                if (!child.isDirectory()) {
                    throw new VfsException("Not a directory: " + child.getAbsolutePath());
                }
                current = child;
            } else {
                Directory newDir = new Directory(part, new Permission("rwxr-xr-x"));
                dir.addChild(newDir);
                current = newDir;
            }
        }
        return (Directory) current;
    }

    public void delete(String path, String workingDir, boolean recursive, boolean force) {
        List<String> parts = normalizePath(path, workingDir);
        if (parts.isEmpty()) {
            throw new VfsException("Cannot delete root directory");
        }
        FileNode node;
        try {
            node = resolve(path, workingDir);
        } catch (VfsException e) {
            if (force) {
                return;
            }
            throw e;
        }
        if (node.isDirectory() && !recursive) {
            throw new VfsException("Cannot remove directory without -r: " + path);
        }
        Directory parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node.getName());
        }
    }

    public void copy(String srcPath, String destPath, String workingDir, boolean recursive) {
        FileNode src = resolve(srcPath, workingDir);
        if (src.isDirectory() && !recursive) {
            throw new VfsException("cp: -r not specified; omitting directory '" + srcPath + "'");
        }

        // Determine destination
        FileNode destNode = null;
        try {
            destNode = resolve(destPath, workingDir);
        } catch (VfsException e) {
            // dest doesn't exist, that's ok
        }

        if (destNode != null && destNode.isDirectory()) {
            // Copy into directory
            Directory destDir = (Directory) destNode;
            FileNode copy = src.deepCopy();
            copy.setName(src.getName());
            destDir.addChild(copy);
        } else if (destNode != null && !destNode.isDirectory()) {
            // Overwrite file
            if (src.isDirectory()) {
                throw new VfsException("Cannot overwrite non-directory with directory");
            }
            RegularFile destFile = (RegularFile) destNode;
            destFile.setContent(((RegularFile) src).getContent());
        } else {
            // Create new node at dest
            Directory parent = resolveParent(destPath, workingDir);
            if (parent == null) {
                throw new VfsException("Cannot copy to root");
            }
            String name = getBaseName(destPath, workingDir);
            FileNode copy = src.deepCopy();
            copy.setName(name);
            parent.addChild(copy);
        }
    }

    public void move(String srcPath, String destPath, String workingDir) {
        FileNode src = resolve(srcPath, workingDir);
        List<String> srcParts = normalizePath(srcPath, workingDir);
        if (srcParts.isEmpty()) {
            throw new VfsException("Cannot move root directory");
        }

        FileNode destNode = null;
        try {
            destNode = resolve(destPath, workingDir);
        } catch (VfsException e) {
            // dest doesn't exist
        }

        Directory srcParent = src.getParent();

        if (destNode != null && destNode.isDirectory()) {
            // Move into directory
            srcParent.removeChild(src.getName());
            ((Directory) destNode).addChild(src);
        } else if (destNode != null && !destNode.isDirectory()) {
            // Overwrite file
            if (src.isDirectory()) {
                throw new VfsException("Cannot overwrite non-directory with directory");
            }
            Directory destParent = destNode.getParent();
            destParent.removeChild(destNode.getName());
            srcParent.removeChild(src.getName());
            String newName = destNode.getName();
            src.setName(newName);
            destParent.addChild(src);
        } else {
            // Rename to new name
            Directory parent = resolveParent(destPath, workingDir);
            String name = getBaseName(destPath, workingDir);
            srcParent.removeChild(src.getName());
            src.setName(name);
            parent.addChild(src);
        }
    }

    public String readFile(String path, String workingDir) {
        FileNode node = resolve(path, workingDir);
        if (node.isDirectory()) {
            throw new VfsException("Is a directory: " + path);
        }
        if (!node.getPermission().canOwnerRead()) {
            throw new VfsException("Permission denied: " + path);
        }
        return ((RegularFile) node).getContent();
    }

    public void writeFile(String path, String workingDir, String content, boolean append) {
        FileNode node;
        try {
            node = resolve(path, workingDir);
        } catch (VfsException e) {
            // File doesn't exist, create it
            node = createFile(path, workingDir);
        }
        if (node.isDirectory()) {
            throw new VfsException("Is a directory: " + path);
        }
        if (!node.getPermission().canOwnerWrite()) {
            throw new VfsException("Permission denied: " + path);
        }
        RegularFile file = (RegularFile) node;
        if (append) {
            file.appendContent(content);
        } else {
            file.setContent(content);
        }
    }

    public List<FileNode> listDirectory(String path, String workingDir, boolean showHidden) {
        FileNode node = resolve(path, workingDir);
        if (!node.isDirectory()) {
            throw new VfsException("Not a directory: " + path);
        }
        if (!node.getPermission().canOwnerRead()) {
            throw new VfsException("Permission denied: " + path);
        }
        List<FileNode> children = ((Directory) node).getChildren();
        if (!showHidden) {
            children = children.stream()
                    .filter(c -> !c.getName().startsWith("."))
                    .collect(java.util.stream.Collectors.toList());
        }
        return children;
    }

    public Directory getRoot() {
        return root;
    }

    public VirtualFileSystem deepCopy() {
        return new VirtualFileSystem(root.deepCopy());
    }

    /**
     * Find nodes by name pattern (supports * wildcard).
     */
    public List<FileNode> findByName(String startPath, String workingDir, String pattern) {
        FileNode start = resolve(startPath, workingDir);
        if (!start.isDirectory()) {
            throw new VfsException("Not a directory: " + startPath);
        }
        List<FileNode> results = new ArrayList<>();
        findRecursive((Directory) start, pattern, results);
        return results;
    }

    private void findRecursive(Directory dir, String pattern, List<FileNode> results) {
        for (FileNode child : dir.getChildren()) {
            if (matchesWildcard(pattern, child.getName())) {
                results.add(child);
            }
            if (child.isDirectory()) {
                findRecursive((Directory) child, pattern, results);
            }
        }
    }

    public static boolean matchesWildcard(String pattern, String name) {
        String regex = "";
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                regex += ".*";
            } else if (c == '?') {
                regex += ".";
            } else if (".+^${}[]|()\\".indexOf(c) >= 0) {
                regex += "\\" + c;
            } else {
                regex += c;
            }
        }
        return name.matches("^" + regex + "$");
    }

    /**
     * Check if a path exists.
     */
    public boolean exists(String path, String workingDir) {
        try {
            resolve(path, workingDir);
            return true;
        } catch (VfsException e) {
            return false;
        }
    }

    /**
     * Normalize a path string into a list of path components.
     */
    List<String> normalizePath(String pathString, String workingDir) {
        String[] rawParts;
        if (pathString.startsWith("/")) {
            rawParts = pathString.split("/");
        } else if (pathString.startsWith("~")) {
            String rest = pathString.length() > 1 ? pathString.substring(2) : "";
            String expanded = "/home/user" + (rest.isEmpty() ? "" : "/" + rest);
            rawParts = expanded.split("/");
        } else {
            String abs = workingDir + "/" + pathString;
            rawParts = abs.split("/");
        }

        List<String> resolved = new ArrayList<>();
        for (String part : rawParts) {
            if (part.isEmpty() || part.equals(".")) {
                // skip empty parts and current-dir references
            } else if (part.equals("..")) {
                if (!resolved.isEmpty()) {
                    resolved.remove(resolved.size() - 1);
                }
            } else {
                resolved.add(part);
            }
        }
        return resolved;
    }

    /**
     * Get the absolute path for a given path string.
     */
    public String getAbsolutePath(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        if (parts.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", parts);
    }
}
