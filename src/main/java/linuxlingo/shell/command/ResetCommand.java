package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Resets the VFS to the default initial state.
 *
 * <p><b>Owner: B</b></p>
 */
public class ResetCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length > 0) {
            return CommandResult.error("reset: usage: " + getUsage());
        }
        session.replaceVfs(new VirtualFileSystem());
        session.setWorkingDir("/");
        session.setPreviousDir(null);
        return CommandResult.success("Environment reset to default.");
    }

    @Override
    public String getUsage() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset VFS to default initial state";
    }
}
