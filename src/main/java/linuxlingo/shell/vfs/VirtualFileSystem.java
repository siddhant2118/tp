package linuxlingo.shell.vfs;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a facade for all virtual file system operations.
 *
 * <p>Supports path resolution, file and directory creation, deletion,
 * copy, move, content read/write, permission checks, wildcard search,
 * and deep-copy cloning. All paths are resolved relative to a given
 * working directory or from the root ({@code "/"}).</p>
 */
public class VirtualFileSystem {
    /** Root directory of the virtual file system. */
    private final Directory root;

    /**
     * Constructs a new virtual file system with the default directory tree.
     */
    public VirtualFileSystem() {
        this.root = createDefaultTree();
    }

    /**
     * Constructs a virtual file system using the supplied root directory.
     *
     * @param root the root directory to use.
     */
    public VirtualFileSystem(Directory root) {
        this.root = root;
    }

    /**
     * Creates the default VFS tree containing {@code /home/user},
     * {@code /tmp}, and {@code /etc/hostname}.
     *
     * @return the newly created root directory.
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
     * Resolves a path string to the corresponding {@link FileNode}.
     *
     * @param path       the absolute or relative path to resolve.
     * @param workingDir the current working directory for relative paths.
     * @return the resolved node.
     * @throws VfsException if the path does not exist or a component is not a directory.
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
     * Resolves the parent directory of the given path.
     *
     * @param path       the path whose parent is to be resolved.
     * @param workingDir the current working directory for relative paths.
     * @return the parent directory, or {@code null} if the path points to root.
     * @throws VfsException if a component in the path does not exist.
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
     * Returns the basename (last component) of the given path.
     *
     * @param path       the path to extract the basename from.
     * @param workingDir the current working directory for relative paths.
     * @return the basename, or an empty string if the path is root.
     */
    public String getBaseName(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        if (parts.isEmpty()) {
            return "";
        }
        return parts.get(parts.size() - 1);
    }

    /**
     * Creates a new regular file at the given path, or returns an existing one
     * (similar to the Unix {@code touch} command).
     *
     * @param path       the path of the file to create.
     * @param workingDir the current working directory for relative paths.
     * @return the created or existing {@link RegularFile}.
     * @throws VfsException if the path points to a directory or permission is denied.
     */
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

    /**
     * Creates a directory at the given path.
     * If {@code parents} is {@code true}, creates intermediate directories as needed
     * (similar to {@code mkdir -p}).
     *
     * @param path       the path of the directory to create.
     * @param workingDir the current working directory for relative paths.
     * @param parents    whether to create parent directories recursively.
     * @return the created or existing directory.
     * @throws VfsException if the path conflicts with an existing file or permission is denied.
     */
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

    /**
     * Recursively creates directories along the given path,
     * similar to {@code mkdir -p}.
     */
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

    /**
     * Deletes the node at the given path.
     *
     * @param path       the path of the node to delete.
     * @param workingDir the current working directory for relative paths.
     * @param recursive  whether to recursively delete directories.
     * @param force      whether to suppress errors for non-existent paths.
     * @throws VfsException if the target is a directory and recursive is {@code false},
     *                      or if the path does not exist and force is {@code false}.
     */
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

    /**
     * Copies a node from the source path to the destination path.
     *
     * @param srcPath    the path of the source node.
     * @param destPath   the path of the destination.
     * @param workingDir the current working directory for relative paths.
     * @param recursive  whether to allow copying directories.
     * @throws VfsException if the source is a directory and recursive is {@code false}.
     */
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

    /**
     * Moves (or renames) a node from the source path to the destination path.
     *
     * @param srcPath    the path of the node to move.
     * @param destPath   the target path or directory.
     * @param workingDir the current working directory for relative paths.
     * @throws VfsException if the root is moved or types conflict.
     */
    public void move(String srcPath, String destPath, String workingDir) {
        FileNode src = resolve(srcPath, workingDir);
        List<String> srcParts = normalizePath(srcPath, workingDir);
        if (srcParts.isEmpty()) {
            throw new VfsException("Cannot move root directory");
        }

        // Check for moving a directory into itself
        String absSrc = getAbsolutePath(srcPath, workingDir);
        String absDest = getAbsolutePath(destPath, workingDir);
        if (absDest.startsWith(absSrc + "/")) {
            throw new VfsException("mv: cannot move '" + srcPath + "' to a subdirectory of itself");
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

    /**
     * Reads and returns the text content of a regular file.
     *
     * @param path       the path of the file to read.
     * @param workingDir the current working directory for relative paths.
     * @return the file content.
     * @throws VfsException if the path is a directory or read permission is denied.
     */
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

    /**
     * Writes content to a regular file, creating it if it does not exist.
     *
     * @param path       the path of the file to write.
     * @param workingDir the current working directory for relative paths.
     * @param content    the content to write.
     * @param append     whether to append to existing content instead of overwriting.
     * @throws VfsException if the path is a directory or write permission is denied.
     */
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

    /**
     * Lists the children of a directory, optionally including hidden entries.
     *
     * @param path       the path of the directory to list.
     * @param workingDir the current working directory for relative paths.
     * @param showHidden whether to include entries starting with {@code '.'}.
     * @return a list of child nodes.
     * @throws VfsException if the path is not a directory or read permission is denied.
     */
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

    /**
     * Returns the root directory of this virtual file system.
     */
    public Directory getRoot() {
        return root;
    }

    /**
     * Creates and returns a deep copy of this entire virtual file system.
     *
     * @return a new {@code VirtualFileSystem} with a cloned directory tree.
     */
    public VirtualFileSystem deepCopy() {
        return new VirtualFileSystem(root.deepCopy());
    }

    /**
     * Finds nodes whose names match the given wildcard pattern, searching
     * recursively from the specified start path.
     *
     * @param startPath  the directory to start searching from.
     * @param workingDir the current working directory for relative paths.
     * @param pattern    the wildcard pattern (supports {@code *} and {@code ?}).
     * @return a list of matching nodes.
     * @throws VfsException if the start path is not a directory.
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

    /** Recursively searches a directory tree for nodes matching the pattern. */
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

    /**
     * Checks whether a name matches a wildcard pattern.
     * Supports {@code *} (any sequence) and {@code ?} (single character).
     *
     * @param pattern the wildcard pattern.
     * @param name    the name to test.
     * @return {@code true} if the name matches the pattern.
     */
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
     * Checks whether a path exists in the virtual file system.
     *
     * @param path       the path to check.
     * @param workingDir the current working directory for relative paths.
     * @return {@code true} if the path resolves to an existing node.
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
     * Normalizes a path string into a list of resolved path components.
     * Handles absolute paths, relative paths, {@code ~} expansion, and
     * {@code .} / {@code ..} resolution.
     *
     * @param pathString the raw path string.
     * @param workingDir the current working directory for relative paths.
     * @return a list of normalised path component strings.
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
     * Returns the absolute path for a given path string by normalising
     * and joining its components.
     *
     * @param path       the raw path string.
     * @param workingDir the current working directory for relative paths.
     * @return the normalised absolute path (e.g. {@code "/home/user"}).
     */
    public String getAbsolutePath(String path, String workingDir) {
        List<String> parts = normalizePath(path, workingDir);
        if (parts.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", parts);
    }
}
