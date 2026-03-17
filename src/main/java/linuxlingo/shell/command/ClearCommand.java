package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Clears the terminal screen.
 *
 * <p><b>Owner: B</b></p>
 */
public class ClearCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        session.getUi().clearScreen();
        return CommandResult.success("");
    }

    @Override
    public String getUsage() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear the screen";
    }
}
