package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Removes files or directories.
 * Syntax: rm [-r] [-f] &lt;path&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class RmCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean recursive = false;
        boolean force = false;
        boolean endOfOptions = false;
        List<String> paths = new ArrayList<>();

        for (String arg : args) {
            if (!endOfOptions && arg.equals("--")) {
                endOfOptions = true;
            } else if (!endOfOptions && arg.equals("-r")) {
                recursive = true;
            } else if (!endOfOptions && arg.equals("-f")) {
                force = true;
            } else if (endOfOptions || !arg.startsWith("-")) {
                paths.add(arg);
            }
        }

        if (paths.isEmpty()) {
            return CommandResult.error("rm: missing operand");
        }

        for (String path : paths) {
            try {
                String absTarget = session.getVfs().getAbsolutePath(path, session.getWorkingDir());
                String absCwd = session.getVfs().getAbsolutePath(session.getWorkingDir(), "/");
                if (absCwd.equals(absTarget) || absCwd.startsWith(absTarget + "/")) {
                    return CommandResult.error("rm: cannot remove '" + path
                            + "': current working directory is inside this directory");
                }
                session.getVfs().delete(path, session.getWorkingDir(), recursive, force);
            } catch (VfsException e) {
                return CommandResult.error("rm: " + e.getMessage());
            }
        }

        return CommandResult.success("");
    }

    @Override
    public String getUsage() {
        return "rm [-r] [-f] <path>";
    }

    @Override
    public String getDescription() {
        return "Remove file or directory";
    }
}
