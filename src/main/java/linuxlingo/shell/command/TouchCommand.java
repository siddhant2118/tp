package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Creates an empty file.
 * <p><b>v1.0</b>: Single file creation.</p>
 * <p><b>v2.0 [TODO]</b>: Support creating multiple files in one command.</p>
 */
public class TouchCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // ===== v1.0 implementation (single file) =====
        if (args.length == 0) {
            return CommandResult.error("touch: missing file operand");
        }
        try {
            session.getVfs().createFile(args[0], session.getWorkingDir());
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("touch: " + e.getMessage());
        }
        // ===== end v1.0 =====

        // TODO [v2.0]: Support creating multiple files.
        //  - Loop over all args and call createFile for each
        //  - Update getUsage() to "touch <file> [file2...]"
    }

    @Override
    public String getUsage() {
        return "touch <file>";
    }

    @Override
    public String getDescription() {
        return "Create an empty file";
    }
}
