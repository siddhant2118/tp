package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.storage.VfsSerializer;

/**
 * Deletes a saved environment.
 * Syntax: envdelete &lt;name&gt;
 *
 * <p><b>Owner: B</b></p>
 */
public class EnvDeleteCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length != 1) {
            return CommandResult.error("envdelete: usage: " + getUsage());
        }
        String name = args[0];
        boolean deleted = VfsSerializer.deleteEnvironment(name);
        if (deleted) {
            return CommandResult.success("Environment deleted: " + name);
        }
        return CommandResult.error("envdelete: environment not found: " + name);
    }

    @Override
    public String getUsage() {
        return "envdelete <name>";
    }

    @Override
    public String getDescription() {
        return "Delete a saved environment";
    }
}
