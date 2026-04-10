package linuxlingo.shell.command;

import java.util.List;
import java.util.logging.Logger;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the in-session command history.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code history}    — lists all commands, numbered from 1.</li>
 *   <li>{@code history N}  — shows the last N commands.</li>
 *   <li>{@code history -c} — clears the command history.</li>
 * </ul>
 */
public class HistoryCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(HistoryCommand.class.getName());

    private static final String HISTORY_FORMAT = "%5d  %s";

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        List<String> history = session.getCommandHistory();

        if (args.length == 0) {
            return formatHistory(history, 0);
        }

        if (args.length > 1) {
            return CommandResult.error("history: too many arguemnts");
        }

        if (args[0].equals("-c")) {
            history.clear();
            LOGGER.fine("Command history cleared");
            return CommandResult.success("");
        }

        return showLastN(history, args[0]);
    }

    /**
     * Returns the last {@code n} history entries as a formatted result.
     * Returns an error result if the argument is not a valid non-negative integer.
     *
     * @param history the current command history list
     * @param nStr    the raw argument string representing {@code N}
     * @return a {@link CommandResult} with the last N entries, or an error
     */
    private CommandResult showLastN(List<String> history, String nStr) {
        int n;
        try {
            n = Integer.parseInt(nStr);
        } catch (NumberFormatException e) {
            return CommandResult.error("history: numeric argument required");
        }

        if ( n < 0) {
            return CommandResult.error("history: invalid option: " + nStr);
        }

        int startIndex = Math.max(0, history.size() - n);
        return formatHistory(history, startIndex);
    }

    /**
     * Formats history entries as a numbered list starting from {@code fromIndex}.
     * Uses two spaces between the number and command to match standard bash output.
     *
     * @param history   the full history list
     * @param fromIndex the index to start listing from (inclusive)
     * @return a {@link CommandResult} containing the formatted output
     */
    private CommandResult formatHistory(List<String> history, int fromIndex) {
        assert fromIndex >= 0 : "fromIndex must be non-negative, got: " + fromIndex;
        assert fromIndex <= history.size() : "fromIndex exceeds history size";

        if (fromIndex >= history.size()) {
            return CommandResult.success("");
        }

        StringBuilder output = new StringBuilder();
        for (int i = fromIndex; i < history.size(); i++) {
            if (!output.isEmpty()) {
                output.append('\n');
            }

            output.append(String.format(HISTORY_FORMAT, i + 1, history.get(i)));
        }
        return CommandResult.success(output.toString());
    }
    @Override
    public String getUsage() {
        return "history [-c] [N]";
    }

    @Override
    public String getDescription() {
        return "Display or manage command history";
    }
}
