package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Creates a directory.
 * <p><b>v1.0</b>: Single directory creation with optional -p flag.</p>
 * <p><b>v2.0 [TODO]</b>: Support multiple directory paths in one command.</p>
 */
public class MkdirCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // ===== v1.0 implementation (single directory) =====
        boolean parents = false;
        String path = null;

        for (String arg : args) {
            if (arg.equals("-p")) {
                parents = true;
            } else if (!arg.startsWith("-")) {
                path = arg;
            }
        }

        if (path == null) {
            return CommandResult.error("mkdir: missing operand");
        }

        try {
            session.getVfs().createDirectory(path, session.getWorkingDir(), parents);
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("mkdir: " + e.getMessage());
        }
        // ===== end v1.0 =====

        // TODO [v2.0]: Support creating multiple directories in a single invocation.
        //  - Collect all non-flag args into a List<String> paths
        //  - Loop over paths and call createDirectory for each
        //  - Update getUsage() to "mkdir [-p] <path> [path2...]"
    }

    @Override
    public String getUsage() {
        return "mkdir [-p] <path>";
    }

    @Override
    public String getDescription() {
        return "Create directory";
    }
}
