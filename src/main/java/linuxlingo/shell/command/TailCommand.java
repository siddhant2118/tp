package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Displays the last N lines of a file (default 10).
 * Syntax: tail [-n N] [-N] &lt;file&gt;
 *
 * <p><b>v1.0</b>: Single file support with -n flag.</p>
 * <p><b>v2.0</b>: Support multiple files with "==>" headers, legacy {@code -N} syntax,
 * and {@code -n +N} to skip first N-1 lines.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class TailCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        int n = 10;
        boolean fromStart = false; // true for -n +N syntax
        boolean endOfOptions = false;
        List<String> files = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (!endOfOptions && args[i].equals("--")) {
                endOfOptions = true;
            } else if (!endOfOptions && args[i].equals("-n")) {
                if (i + 1 >= args.length) {
                    return CommandResult.error("tail: option requires an argument -- n");
                }

                String nArg = args[i + 1];
                i++;

                if (nArg.startsWith("+")) {
                    // -n +N: output starting from line N
                    fromStart = true;
                    try {
                        n = Integer.parseInt(nArg.substring(1));
                    } catch (NumberFormatException e) {
                        return CommandResult.error("tail: invalid number of lines: " + nArg);
                    }
                } else {
                    try {
                        n = Integer.parseInt(nArg);
                    } catch (NumberFormatException e) {
                        return CommandResult.error("tail: invalid number of lines: " + nArg);
                    }

                    if (n < 0) {
                        return CommandResult.error("tail: invalid number of lines: " + nArg);
                    }
                }
            } else if (!endOfOptions && args[i].matches("-\\d+")) {
                // Legacy -N syntax: -5 means -n 5
                try {
                    n = Integer.parseInt(args[i].substring(1));
                } catch (NumberFormatException e) {
                    return CommandResult.error("tail: invalid number of lines: " + args[i]);
                }
            } else {
                files.add(args[i]);
            }
        }

        if (files.isEmpty() && stdin == null) {
            return CommandResult.error("tail: missing file operand");
        }

        StringBuilder sb = new StringBuilder();
        boolean hasError = false;

        boolean multiFile = files.size() > 1;

        if (!files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                try {
                    String content = session.getVfs().readFile(files.get(i), session.getWorkingDir());
                    if (multiFile) {
                        if (i > 0) {
                            sb.append("\n"); // newline between files
                        }
                        sb.append("==> ").append(files.get(i)).append(" <==").append("\n");
                    }
                    if (fromStart) {
                        appendTailFromStart(sb, content, n);
                    } else {
                        appendTailLines(sb, content, n);
                    }
                } catch (VfsException e) {
                    sb.append("tail: ").append(files.get(i)).append(": ").append(e.getMessage());
                    hasError = true;
                }
            }
        } else {
            if (fromStart) {
                appendTailFromStart(sb, stdin, n);
            } else {
                appendTailLines(sb, stdin, n);
            }
        }

        String result = sb.toString();
        return hasError ? CommandResult.error(result) : CommandResult.success(result);
    }

    private void appendTailLines(StringBuilder sb, String content, int n) {
        if (content.isEmpty()) {
            return;
        }

        String[] linesArray = content.split("\n", -1);
        boolean endsWithNewline = content.endsWith("\n");
        int len = endsWithNewline ? linesArray.length - 1 : linesArray.length;
        int start = Math.max(0, len - n);

        for (int i = start; i < len; i++) {
            sb.append(linesArray[i]);

            boolean isLastLine = i == len - 1;
            if (!isLastLine || endsWithNewline) {
                sb.append("\n");
            }
        }
    }

    /**
     * Appends lines starting from line N (1-indexed), skipping the first N-1 lines.
     *
     * @param sb      the StringBuilder to append to
     * @param content the file content
     * @param n       the starting line number (1-indexed)
     */
    private void appendTailFromStart(StringBuilder sb, String content, int n) {
        if (content.isEmpty()) {
            return;
        }

        String[] linesArray = content.split("\n", -1);
        boolean endsWithNewline = content.endsWith("\n");
        int len = endsWithNewline ? linesArray.length - 1 : linesArray.length;
        int start = Math.max(0, n - 1);

        for (int i = start; i < len; i++) {
            sb.append(linesArray[i]);

            boolean isLastLine = i == len - 1;
            if (!isLastLine || endsWithNewline) {
                sb.append("\n");
            }
        }
    }

    @Override
    public String getUsage() {
        return "tail [-n N] [-N] <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Display last N lines of a file (default 10)";
    }
}
