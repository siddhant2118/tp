package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.storage.StorageException;
import linuxlingo.storage.VfsSerializer;

/**
 * Saves the current VFS state to a named environment file.
 * Syntax: save &lt;name&gt;
 *
 * <p><b>Owner: B</b></p>
 */
public class SaveCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length != 1) {
            return CommandResult.error("save: usage: " + getUsage());
        }
        String name = args[0];
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            return CommandResult.error("save: invalid environment name: " + name);
        }
        try {
            VfsSerializer.saveToFile(session.getVfs(), session.getWorkingDir(), name);
            return CommandResult.success("Environment saved: " + name);
        } catch (StorageException e) {
            return CommandResult.error("save: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "save <name>";
    }

    @Override
    public String getDescription() {
        return "Save the current VFS state to a named snapshot";
    }
}
