package linuxlingo.shell.command;

import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.storage.VfsSerializer;

/**
 * Lists all saved environment names.
 *
 * <p><b>Owner: B</b></p>
 */
public class EnvListCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length > 0) {
            return CommandResult.error("envlist: usage: " + getUsage());
        }
        List<String> names = VfsSerializer.listEnvironments();
        if (names.isEmpty()) {
            return CommandResult.success("No saved environments.");
        }
        StringBuilder output = new StringBuilder("Saved environments:");
        for (String name : names) {
            output.append("\n  ").append(name);
        }
        return CommandResult.success(output.toString());
    }

    @Override
    public String getUsage() {
        return "envlist";
    }

    @Override
    public String getDescription() {
        return "List all saved environment names";
    }
}
