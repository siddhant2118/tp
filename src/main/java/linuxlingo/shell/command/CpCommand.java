package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Copies files or directories.
 * Syntax: cp [-r] &lt;src...&gt; &lt;dest&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class CpCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean recursive = false;
        boolean endOfOptions = false;
        List<String> paths = new ArrayList<>();

        for (String arg : args) {
            if (!endOfOptions && arg.equals("--")) {
                endOfOptions = true;
            } else if (!endOfOptions && arg.equals("-r")) {
                recursive = true;
            } else if (endOfOptions || !arg.startsWith("-")) {
                paths.add(arg);
            }
        }

        if (paths.size() < 2) {
            return CommandResult.error("cp: " + getUsage());
        }

        // Multi-source: if more than 2 paths, last must be an existing directory
        if (paths.size() > 2) {
            String dest = paths.get(paths.size() - 1);
            if (!session.getVfs().exists(dest, session.getWorkingDir())
                    || !session.getVfs().resolve(dest, session.getWorkingDir()).isDirectory()) {
                return CommandResult.error("cp: target '" + dest + "' is not a directory");
            }
            for (int i = 0; i < paths.size() - 1; i++) {
                try {
                    CommandResult validationError = validateCopy(session, paths.get(i), dest);
                    if (validationError != null) {
                        return validationError;
                    }
                    session.getVfs().copy(paths.get(i), dest, session.getWorkingDir(), recursive);
                } catch (VfsException e) {
                    return CommandResult.error("cp: " + e.getMessage());
                }
            }
            return CommandResult.success("");
        }

        try {
            CommandResult validationError = validateCopy(session, paths.get(0), paths.get(1));
            if (validationError != null) {
                return validationError;
            }
            session.getVfs().copy(paths.get(0), paths.get(1), session.getWorkingDir(), recursive);
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("cp: " + e.getMessage());
        }
    }

    private CommandResult validateCopy(ShellSession session, String srcPath, String destPath) {
        String absSrc = session.getVfs().getAbsolutePath(srcPath, session.getWorkingDir());
        String absDest = session.getVfs().getAbsolutePath(destPath, session.getWorkingDir());
        if (absSrc.equals(absDest)) {
            return CommandResult.error("cp: '" + srcPath + "' and '" + destPath + "' are the same file");
        }

        try {
            var srcNode = session.getVfs().resolve(srcPath, session.getWorkingDir());
            var destNode = session.getVfs().resolve(destPath, session.getWorkingDir());
            if (srcNode.isDirectory() && destNode.isDirectory()) {
                String finalDest = "/".equals(absSrc)
                        ? absDest
                        : absDest + (absDest.endsWith("/") ? "" : "/") + srcNode.getName();
                if (isSameOrDescendant(finalDest, absSrc)) {
                    return CommandResult.error("cp: cannot copy a directory, '" + srcPath
                            + "', into itself, '" + destPath + "'");
                }
            }
        } catch (VfsException e) {
            // Destination may not exist yet; fall through to raw-path validation below.
        }

        if (isSameOrDescendant(absDest, absSrc)) {
            try {
                if (session.getVfs().resolve(srcPath, session.getWorkingDir()).isDirectory()) {
                    return CommandResult.error("cp: cannot copy a directory, '" + srcPath
                            + "', into itself, '" + destPath + "'");
                }
            } catch (VfsException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isSameOrDescendant(String path, String ancestor) {
        if (path.equals(ancestor)) {
            return true;
        }
        if ("/".equals(ancestor)) {
            return path.startsWith("/");
        }
        return path.startsWith(ancestor + "/");
    }

    @Override
    public String getUsage() {
        return "cp [-r] <src...> <dest>";
    }

    @Override
    public String getDescription() {
        return "Copy file or directory";
    }
}
