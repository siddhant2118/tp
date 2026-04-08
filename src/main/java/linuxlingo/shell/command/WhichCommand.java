package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Shows whether commands are available in the shell registry.
 * Syntax: {@code which <command> [command2 ...]}.
 *
 * <p>For each queried command, this implementation prints a simulated path
 * ({@code /bin/<name>}) when found, or {@code <name>: not found} when missing.</p>
 */
public class WhichCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length < 1) {
            return CommandResult.error("which: missing operand");
        }

        StringBuilder sb = new StringBuilder();

        for (String arg : args) {
            if (session.getRegistry().get(arg) != null) {
                sb.append(arg).append(": /bin/").append(arg).append("\n");
            } else {
                sb.append(arg).append(": not found").append("\n");
            }
        }
        return CommandResult.success(sb.toString());
    }

    @Override
    public String getUsage() {
        return "which <command>";
    }

    @Override
    public String getDescription() {
        return "Show the path of a command";
    }
}
