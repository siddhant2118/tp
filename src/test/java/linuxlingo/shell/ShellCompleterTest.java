package linuxlingo.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.jline.reader.Candidate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Tests for ShellCompleter (stub verification).
 *
 * <h3>v2.0 (stub)</h3>
 * <p>All completion methods currently return empty collections.
 * Tests marked {@code @Disabled} document expected behaviour once
 * the completer is fully implemented.</p>
 */
public class ShellCompleterTest {
    private VirtualFileSystem vfs;
    private ShellSession session;
    private ShellCompleter completer;

    @BeforeEach
    public void setUp() {
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        session.setWorkingDir("/home/user");
        completer = new ShellCompleter(session);
    }

    // ─── Stub verification ──────────────────────────────────────

    @Test
    public void getCommandCompletions_stub_returnsEmpty() {
        SortedSet<String> results = completer.getCommandCompletions("");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void getPathCompletions_stub_returnsEmpty() {
        SortedSet<String> results = completer.getPathCompletions("/");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void completeCommandName_stub_noCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        completer.completeCommandName("gr", candidates);
        assertTrue(candidates.isEmpty());
    }

    @Test
    public void completePath_stub_noCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        completer.completePath("da", candidates);
        assertTrue(candidates.isEmpty());
    }

    // ─── @Disabled: document expected full behaviour ────────────

    @Nested
    @Disabled("v2.0 — command completion to be implemented")
    class CommandNameCompletion {
        @Test
        public void emptyPrefix_returnsAllCommands() {
            SortedSet<String> results = completer.getCommandCompletions("");
            assertTrue(results.contains("ls"));
            assertTrue(results.contains("cd"));
        }

        @Test
        public void prefixL_returnsLCommands() {
            SortedSet<String> results = completer.getCommandCompletions("l");
            assertTrue(results.contains("ls"));
        }
    }

    @Nested
    @Disabled("v2.0 — path completion to be implemented")
    class PathCompletion {
        @Test
        public void absolutePath_rootChildren() {
            SortedSet<String> results = completer.getPathCompletions("/");
            assertTrue(results.contains("/home/"));
        }

        @Test
        public void relativePath_fromWorkingDir() {
            vfs.createFile("/home/user/notes.txt", "/");
            SortedSet<String> results = completer.getPathCompletions("n");
            assertTrue(results.contains("notes.txt"));
        }
    }

    @Nested
    @Disabled("v2.0 — candidate integration to be implemented")
    class CandidateIntegration {
        @Test
        public void completeCommandName_addsCandidates() {
            List<Candidate> candidates = new ArrayList<>();
            completer.completeCommandName("gr", candidates);
            assertTrue(candidates.stream().anyMatch(c -> c.value().equals("grep")));
        }
    }
}
