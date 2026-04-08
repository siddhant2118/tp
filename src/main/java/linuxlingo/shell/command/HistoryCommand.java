package linuxlingo.shell.command;

import java.util.List;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the command history.
 *
 * <p>Usage: {@code history} — lists all commands in the current session history.</p>
 * <p>Usage: {@code history -c} — clears the command history.</p>
 * <p>Usage: {@code history N} — shows the last N commands.</p>
 *
 * <p>Supports listing all entries, clearing history with {@code -c}, and
 * limiting output to the last {@code N} commands.</p>
 */
public class HistoryCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        List<String> history = session.getCommandHistory();

        if (args.length > 0 && args[0].equals("-c")) {
            history.clear();
            return CommandResult.success("");
        }

        if (args.length > 0) {
            return showLastN(history, args[0]);
        }

        return formatHistory(history, 0);
    }

    /**
     * Returns up to the last {@code n} history entries.
     *
     * @param history source command history
     * @param nStr user-provided count
     * @return limited history output or an error when {@code nStr} is invalid
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
     * Formats history entries as a numbered list from {@code fromIndex}.
     *
     * @param history source command history
     * @param fromIndex zero-based start index
     * @return formatted history output
     */
    private CommandResult formatHistory(List<String> history, int fromIndex) {
        if (history.isEmpty()) {
            return CommandResult.success("");
        }

        StringBuilder sbuild = new StringBuilder();
        for (int i = fromIndex; i < history.size(); i++) {
            if (sbuild.length() > 0) {
                sbuild.append('\n');
            }
            sbuild.append(String.format("%5d %s", i + 1, history.get(i)));
        }
        return CommandResult.success(sbuild.toString());
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
