package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.storage.StorageException;
import linuxlingo.storage.VfsSerializer;

/**
 * Saves the current VFS state to a named environment file.
 * Syntax: save [-f] &lt;name&gt;
 *
 * <p>By default, fails if an environment with the given name already exists,
 * to prevent silent data loss. Use {@code -f} (force) to overwrite.</p>
 *
 * <p><b>Owner: B</b></p>
 */
public class SaveCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean force = false;
        String name = null;
        for (String arg : args) {
            if ("-f".equals(arg) || "--force".equals(arg)) {
                force = true;
            } else if (name == null) {
                name = arg;
            } else {
                return CommandResult.error("save: usage: " + getUsage());
            }
        }
        if (name == null) {
            return CommandResult.error("save: usage: " + getUsage());
        }
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            return CommandResult.error("save: invalid environment name: " + name);
        }
        if (!force && VfsSerializer.environmentExists(name)) {
            return CommandResult.error(
                    "save: environment '" + name + "' already exists. "
                            + "Use 'save -f " + name + "' to overwrite.");
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
        return "save [-f] <name>";
    }

    @Override
    public String getDescription() {
        return "Save the current VFS state to a named snapshot";
    }
}
