package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Creates a directory.
 * <p><b>v1.0</b>: Single directory creation with optional -p flag.</p>
 * <p><b>v2.0</b>: Supports creating multiple directory paths in one command.</p>
 */
public class MkdirCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean parents = false;
        boolean endOfOptions = false;
        List<String> paths = new ArrayList<>();

        for (String arg : args) {
            if (!endOfOptions && arg.equals("--")) {
                endOfOptions = true;
            } else if (!endOfOptions && arg.equals("-p")) {
                parents = true;
            } else if (!endOfOptions && arg.startsWith("-")) {
                return CommandResult.error("mkdir: invalid option -- " + arg);
            } else {
                if (arg.isEmpty()) {
                    return CommandResult.error("mkdir: invalid file name");
                }
                paths.add(arg);
            }
        }

        if (paths.isEmpty()) {
            return CommandResult.error("mkdir: missing operand");
        }

        try {
            for (String path : paths) {
                session.getVfs().createDirectory(path, session.getWorkingDir(), parents);
            }
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("mkdir: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "mkdir [-p] <path> [path2...]";
    }

    @Override
    public String getDescription() {
        return "Create directory";
    }
}
