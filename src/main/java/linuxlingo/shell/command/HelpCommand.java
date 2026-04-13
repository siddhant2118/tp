package linuxlingo.shell.command;

import java.util.Map;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays available commands and their brief usage.
 * Supports: help [command]
 *
 * <p><b>Owner: B</b></p>
 */
public class HelpCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            StringBuilder out = new StringBuilder("Available commands:");
            for (Map.Entry<String, String> entry : session.getRegistry().getHelpText().entrySet()) {
                out.append(String.format("\n  %-20s %s", entry.getKey(), entry.getValue()));
            }
            return CommandResult.success(out.toString());
        }

        Command command = session.getRegistry().get(args[0]);
        if (command == null) {
            return CommandResult.error("help: unknown command: " + args[0]);
        }
        return CommandResult.success("Usage: " + command.getUsage() + "\n" + command.getDescription());
    }

    @Override
    public String getUsage() {
        return "help [command]";
    }

    @Override
    public String getDescription() {
        return "Display available commands and their usage";
    }
}
