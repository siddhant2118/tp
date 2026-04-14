package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Removes adjacent duplicate lines.
 * Syntax: uniq [-c] &lt;file&gt;
 *
 * <p><b>v1.0</b>: Remove adjacent duplicates with optional {@code -c} (count) flag.</p>
 * <p><b>v2.0</b>: Adds {@code -d} to show only duplicated groups.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class UniqCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean countOccurrences = false;
        boolean duplicatesOnly = false;
        boolean endOfOptions = false;

        String file = null;

        for (String arg : args) {
            if (!endOfOptions && arg.equals("--")) {
                endOfOptions = true;
            } else if (!endOfOptions && arg.equals("-c")) {
                countOccurrences = true;
            } else if (!endOfOptions && arg.equals("-d")) {
                duplicatesOnly = true;
            } else if ((endOfOptions || !arg.startsWith("-")) && file == null) {
                file = arg;
            }
        }

        String content;
        if (file != null) {
            try {
                content = session.getVfs().readFile(file, session.getWorkingDir());
            } catch (VfsException e) {
                return CommandResult.error("uniq: " + e.getMessage());
            }
        } else if (stdin != null) {
            content = stdin;
        } else {
            return CommandResult.error("uniq: missing file operand");
        }

        if (content.isEmpty()) {
            return CommandResult.success("");
        }

        StringBuilder sb = new StringBuilder();
        String[] linesArray = content.split("\n", -1);
        boolean endsWithNewline = content.endsWith("\n");
        int len = endsWithNewline ? linesArray.length - 1 : linesArray.length;

        if (len > 0) {
            String currentLine = linesArray[0];
            int count = 1;

            for (int i = 1; i < len; i++) {
                if (linesArray[i].equals(currentLine)) {
                    count++;
                } else {
                    addUniqResult(sb, currentLine, count, countOccurrences, duplicatesOnly);
                    currentLine = linesArray[i];
                    count = 1;
                }
            }

            addUniqResult(sb, currentLine, count, countOccurrences, duplicatesOnly);
        }

        // remove a single trailing \n if present
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return CommandResult.success(sb.toString());
    }

    /**
     * Adds a formatted uniq result line, respecting {@code -c} and {@code -d}.
     *
     * @param sb               the StringBuilder to append to
     * @param line             the current unique line
     * @param count            how many adjacent occurrences
     * @param countOccurrences whether -c flag is set
     * @param duplicatesOnly   whether -d flag is set
     */
    private void addUniqResult(StringBuilder sb, String line, int count,
                               boolean countOccurrences, boolean duplicatesOnly) {
        if (duplicatesOnly && count < 2) {
            return;
        }

        if (countOccurrences) {
            sb.append(String.format("%7d %s\n", count, line));
        } else {
            sb.append(line).append("\n");
        }
    }

    @Override
    public String getUsage() {
        return "uniq [-c] [-d] <file>";
    }

    @Override
    public String getDescription() {
        return "Remove adjacent duplicate lines";
    }
}
