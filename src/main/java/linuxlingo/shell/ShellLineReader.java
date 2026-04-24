package linuxlingo.shell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Wrapper around JLine's {@link LineReader} that provides:
 * <ul>
 *   <li>Tab completion via {@link ShellCompleter}</li>
 *   <li>Persistent command history (up/down arrow keys)</li>
 *   <li>Graceful fallback when JLine terminal cannot be created</li>
 * </ul>
 *
 * <p><b>Owner: B — stub; to be implemented.</b></p>
 *
 * <p>Create via {@link #create(ShellSession)} for interactive use, or
 * via {@link #createDumb(ShellSession)} for testing/non-interactive use.</p>
 *
 */
public class ShellLineReader {
    private static final Logger LOGGER = Logger.getLogger(ShellLineReader.class.getName());

    private final LineReader lineReader;
    private final Terminal terminal;
    private final DefaultHistory history;
    private boolean lastReadReachedEof;

    ShellLineReader(LineReader lineReader, Terminal terminal, DefaultHistory history) {
        this.lineReader = lineReader;
        this.terminal = terminal;
        this.history = history;
        this.lastReadReachedEof = false;
    }

    /**
     * Create an interactive ShellLineReader with system terminal.
     * Falls back to a dumb terminal if the system terminal is unavailable.
     *
     * @param session the shell session to provide completions for
     * @return a new ShellLineReader
     */
    public static ShellLineReader create(ShellSession session) {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .encoding(StandardCharsets.UTF_8)
                    .build();
            return buildReader(session, terminal);
        } catch (IOException e) {
            LOGGER.warning("Failed to create system terminal, using dumb terminal fallback");
            return createDumb(session);
        }
    }

    /**
     * Create a ShellLineReader with a dumb terminal (for testing or non-interactive use).
     *
     * @param session the shell session to provide completions for
     * @return a new ShellLineReader with dumb terminal
     */
    public static ShellLineReader createDumb(ShellSession session) {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .dumb(true)
                    .encoding(StandardCharsets.UTF_8)
                    .build();
            return buildReader(session, terminal);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dumb terminal", e);
        }
    }

    private static ShellLineReader buildReader(ShellSession session, Terminal terminal) {
        DefaultHistory history = new DefaultHistory();
        ShellCompleter completer = new ShellCompleter(session);
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .history(history)
                .build();

        // Disable audible bell, use visible bell instead
        reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);

        // Disable JLine's backslash escaping so that backslashes are passed
        // through literally to ShellParser (which handles its own escaping).
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);

        // Auto-list all completion candidates on an ambiguous completion
        // instead of asking "do you wish to see all N possibilities?". The
        // prompt was visually concatenating with the user's next keystroke
        // and produced confusing output when a user hit TAB with empty
        // input and then typed something (PE issue #30). The whole command
        // set is ~35 items, so auto-listing is fine.
        reader.setOpt(LineReader.Option.AUTO_LIST);
        reader.setVariable(LineReader.LIST_MAX, 1000);

        return new ShellLineReader(reader, terminal, history);
    }

    /**
     * Read a line of input with the given prompt.
     *
     * @param prompt the prompt string to display
     * @return the line read, or {@code null} on EOF (Ctrl-D)
     */
    public String readLine(String prompt) {
        try {
            lastReadReachedEof = false;
            return lineReader.readLine(prompt);
        } catch (EndOfFileException e) {
            lastReadReachedEof = true;
            return null;
        } catch (UserInterruptException e) {
            lastReadReachedEof = false;
            return "";
        } catch (RuntimeException e) {
            lastReadReachedEof = false;
            LOGGER.warning(() -> "JLine read error: " + e.getMessage());
            return "";
        }
    }

    /**
     * Returns whether the most recent {@link #readLine(String)} ended due to EOF.
     */
    public boolean lastReadReachedEof() {
        return lastReadReachedEof;
    }

    /**
     * Get the command history as an unmodifiable list of strings.
     * Entries are in chronological order (oldest first).
     *
     * @return list of history entries
     */
    public List<String> getHistory() {
        List<String> entries = new ArrayList<>();
        for (History.Entry entry : history) {
            entries.add(entry.line());
        }
        return Collections.unmodifiableList(entries);
    }

    /**
     * Get the number of entries in the history.
     *
     * @return history size
     */
    public int getHistorySize() {
        return history.size();
    }

    /**
     * Add a line to the history manually (useful for testing).
     *
     * @param line the command line to add
     */
    public void addToHistory(String line) {
        if (line == null) {
            return;
        }
        history.add(line);
    }

    /**
     * Get the underlying JLine LineReader (for advanced usage).
     *
     * @return the JLine LineReader
     */
    public LineReader getJLineReader() {
        return lineReader;
    }

    /**
     * Close the terminal when done.
     */
    public void close() {
        try {
            terminal.close();
        } catch (IOException e) {
            LOGGER.warning(() -> "Failed to close terminal: " + e.getMessage());
        }
    }
}
