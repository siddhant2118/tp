package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Creates an empty file.
 * <p><b>v1.0</b>: Single file creation.</p>
 * <p><b>v2.0</b>: Supports creating multiple files in one command.</p>
 */
public class TouchCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean endOfOptions = false;
        java.util.List<String> files = new java.util.ArrayList<>();
        for (String arg : args) {
            if (!endOfOptions && arg.equals("--")) {
                endOfOptions = true;
                continue;
            }
            if (arg.isEmpty()) {
                return CommandResult.error("touch: invalid file name");
            }
            files.add(arg);
        }

        if (files.isEmpty()) {
            return CommandResult.error("touch: missing file operand");
        }
        try {
            for (String arg : files) {
                session.getVfs().createFile(arg, session.getWorkingDir());
            }
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("touch: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "touch <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Create an empty file";
    }
}
