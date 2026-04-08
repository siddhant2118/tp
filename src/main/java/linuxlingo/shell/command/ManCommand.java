package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the manual page for a given command.
 * Syntax: {@code man <command>}.
 *
 * <p>The output includes NAME, SYNOPSIS, and DESCRIPTION sections generated
 * from the command's {@code getUsage()} and {@code getDescription()} metadata.</p>
 */
public class ManCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            return CommandResult.error("man: What manual page do you want?");
        }

        Command cmd = session.getRegistry().get(args[0]);
        if (cmd == null) {
            return CommandResult.error("man: No manual entry for " + args[0]);
        }

        String output = "NAME\n    " + args[0] + " - " + cmd.getDescription() + "\n\n" +
                "SYNOPSIS\n    " + cmd.getUsage() + "\n\n" +
                "DESCRIPTION\n    " + cmd.getDescription();
        return CommandResult.success(output);
    }

    @Override
    public String getUsage() {
        return "man <command>";
    }

    @Override
    public String getDescription() {
        return "Display manual page for a command";
    }
}
