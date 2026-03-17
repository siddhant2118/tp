package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.storage.StorageException;
import linuxlingo.storage.VfsSerializer;

/**
 * Loads a previously saved VFS environment and replaces the current one.
 * Syntax: load &lt;name&gt;
 *
 * <p><b>Owner: B</b></p>
 */
public class LoadCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length != 1) {
            return CommandResult.error("load: usage: " + getUsage());
        }
        String name = args[0];
        try {
            VfsSerializer.DeserializedVfs result = VfsSerializer.loadFromFile(name);
            session.replaceVfs(result.getVfs());
            session.setWorkingDir(result.getWorkingDir());
            session.setPreviousDir(null);
            return CommandResult.success("Environment loaded: " + name);
        } catch (StorageException e) {
            return CommandResult.error("load: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "load <name>";
    }

    @Override
    public String getDescription() {
        return "Load a previously saved VFS snapshot";
    }
}
