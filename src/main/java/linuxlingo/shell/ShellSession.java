package linuxlingo.shell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.cli.Ui;
import linuxlingo.shell.command.Command;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Manages the lifecycle of a shell session (interactive REPL + one-shot execution).
 *
 * <h3>v1.0 (implemented)</h3>
 * <p><b>Owner: A — implement start(), executePlan(), and executePlanSilent().</b></p>
 *
 * <p>The getters / setters and the constructor are provided. You need to implement
 * the REPL loop and the plan execution engine that chains pipes, redirections,
 * {@code &&}, and {@code ;} operators.</p>
 *
 * <p><b>Dependency:</b> Falls back to {@link Ui#readLine(String)} for reading input.
 * (Tab-completion via ShellCompleter is deferred beyond v1.0.)</p>
 *
 * <h3>v2.0 Enhancements</h3>
 *
 * <h4>Owner: A — ShellSession core + alias/history commands</h4>
 * <ul>
 *   <li>Alias resolution in runPlan()</li>
 *   <li>{@code ||} (OR) operator support in runPlan()</li>
 *   <li>{@code <} input redirect support in runPlan()</li>
 *   <li>In-memory command history tracking in start()</li>
 *   <li>AliasCommand, UnaliasCommand, HistoryCommand</li>
 * </ul>
 *
 * <h4>Owner: B — independent algorithm modules</h4>
 * <ul>
 *   <li>{@link #suggestCommand(String)} — "Did you mean?" suggestion</li>
 *   <li>{@link #editDistance(String, String)} — Levenshtein algorithm</li>
 *   <li>{@link #expandGlobs(String[])} / expandSingleGlob() — glob expansion</li>
 *   <li>{@link ShellCompleter} — JLine tab-completion</li>
 *   <li>{@link ShellLineReader} — JLine terminal wrapper</li>
 *   <li>startInteractive() — JLine integration</li>
 * </ul>
 *
 * TODO: Member A should implement:
 * - Alias resolution in runPlan()
 * - || (OR) operator handling in runPlan()
 * - < input redirect handling in runPlan()
 * - Command history tracking in start()
 * - AliasCommand, UnaliasCommand, HistoryCommand
 *
 * TODO: Member B should implement:
 * - suggestCommand() / editDistance() — "Did you mean?" algorithm
 * - expandGlobs() / expandSingleGlob() — glob expansion
 * - startInteractive() — JLine integration
 * - ShellCompleter — tab-completion
 * - ShellLineReader — terminal wrapper & history
 */
public class ShellSession {

    private static final Logger LOGGER = Logger.getLogger(ShellSession.class.getName());

    private VirtualFileSystem vfs;
    private String workingDir;
    private String previousDir;
    private int lastExitCode;
    private final CommandRegistry registry;
    private final Ui ui;
    private boolean running;
    private final Map<String, String> aliases;
    // v2.0: will be ShellLineReader once Owner B implements it
    private Object lineReader;
    private final List<String> commandHistory;

    public ShellSession(VirtualFileSystem vfs, Ui ui) {
        if (vfs == null) {
            throw new IllegalArgumentException("ShellSession: vfs must not be null");
        }

        this.vfs = vfs;
        this.ui = ui;
        this.workingDir = "/";
        this.previousDir = null;
        this.lastExitCode = 0;
        this.registry = new CommandRegistry();
        this.running = false;
        this.aliases = new LinkedHashMap<>();
        this.lineReader = null;
        this.commandHistory = new ArrayList<>();

        LOGGER.fine("ShellSession initialised with workingDir='/'");
    }

    /**
     * Enter interactive shell REPL.
     *
     * <h4>v1.0 (implemented)</h4>
     * <ol>
     *   <li>Set {@code running = true}; print welcome message.</li>
     *   <li>Read input using {@link Ui#readLine(String)} with the shell prompt.</li>
     *   <li>Loop: read line → handle special words ("back", "exit", "done") → call
     *       {@link #executePlan(String)}.</li>
     * </ol>
     *
     * <h4>v2.0 TODO</h4>
     * <p>If a {@link ShellLineReader} has been set via {@link #setLineReader},
     * input should be read from JLine (with tab-completion and history).
     * Otherwise, falls back to {@link Ui#readLine(String)}.
     * Also track each command in {@code commandHistory}.</p>
     */
    public void start() {
        assert !running : "start() called while session is already running";

        running = true;
        LOGGER.info("Shell session started");
        ui.println("Welcome to LinuxLingo Shell! Type 'exit' to quit.");

        while (running) {
            // v1.0: read from Ui
            // TODO v2.0 (Owner B): if lineReader != null, use lineReader.readLine(getPrompt()) instead
            String input = ui.readLine(getPrompt());

            // null signals end of piped test input
            if (input == null) {
                running = false;
                break;
            }

            // Skip blank lines and redisplay the prompt
            if (input.trim().isEmpty()) {
                continue;
            }

            // Exit keyword stops the REPL
            String trimmed = input.trim();
            if (trimmed.equalsIgnoreCase("exit")) {
                LOGGER.info("Exit keyword received... stopping REPL");
                running = false;
                break;
            }

            // TODO v2.0 (Owner A): track command in commandHistory
            // commandHistory.add(trimmed);

            executePlan(input);
        }
        LOGGER.log(Level.INFO, "Shell session ended with lastExitCode={0}", lastExitCode);
    }

    /**
     * Start an interactive shell with JLine tab-completion and command history.
     * Creates a {@link ShellLineReader} with a system terminal, then delegates
     * to {@link #start()}.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
     * <p>If JLine cannot initialise (e.g. no TTY), falls back to plain Ui input.</p>
     *
     * TODO: Member B — implement JLine integration:
     * - Create ShellLineReader.create(this) 
     * - Call start()
     * - Close lineReader in finally block
     */
    public void startInteractive() {
        // TODO v2.0: implement JLine integration
        // Placeholder: just start with plain Ui for now
        start();
    }

    /**
     * Execute a single command string silently (for programmatic / exam use).
     * Returns the final {@link CommandResult} without printing.
     *
     * @param input raw command string
     * @return the result of the last segment
     */
    public CommandResult executeOnce(String input) {
        LOGGER.fine(() -> "executeOnce called with: " + input);
        return executePlanSilent(input);
    }

    /**
     * Execute a parsed plan and print output to the UI.
     *
     * <h4>v1.0 Algorithm outline:</h4>
     * <ol>
     *   <li>Parse input with {@link ShellParser#parse(String)}.</li>
     *   <li>Iterate segments. For each:
     *     <ul>
     *       <li>Check preceding operator: if {@code &&} and lastExitCode ≠ 0, break.</li>
     *       <li>Look up command from registry. If not found → print error, set exitCode=127.</li>
     *       <li>If previous operator was {@code |}, pass previous stdout as stdin.</li>
     *       <li>Call {@code command.execute(this, args, stdin)}.</li>
     *       <li>Handle redirect: write stdout to file via
     *           {@link VirtualFileSystem#writeFile(String, String, String, boolean)}.</li>
     *       <li>Print stderr immediately.</li>
     *     </ul>
     *   </li>
     *   <li>After loop, print final stdout.</li>
     * </ol>
     */
    private void executePlan(String input) {
        CommandResult result = runPlan(input);
        // Print the final stdout produced by the last segment
        if (result != null && !result.getStdout().isEmpty()) {
            ui.println(result.getStdout());
        }
    }

    /**
     * Silent variant of {@link #executePlan(String)} — returns result instead of printing.
     */
    private CommandResult executePlanSilent(String input) {
        return runPlan(input);
    }

    /**
     * Core plan execution engine shared by both {@link #executePlan} and
     * {@link #executePlanSilent}.
     *
     * <h4>v1.0 Operator semantics (implemented):</h4>
     * <ul>
     *   <li>{@code PIPE}      — stdout of segment N becomes stdin of segment N+1</li>
     *   <li>{@code AND}       — segment N+1 is skipped if lastExitCode != 0</li>
     *   <li>{@code SEMICOLON} — segment N+1 always runs regardless of exit code</li>
     * </ul>
     *
     * <h4>v2.0 TODO — additional semantics to implement:</h4>
     * <ul>
     *   <li>{@code OR}        — segment N+1 is skipped if lastExitCode == 0</li>
     *   <li>Alias resolution: check aliases map before registry lookup</li>
     *   <li>Input redirect: read file content as stdin when segment.inputRedirect is set</li>
     *   <li>Glob expansion: expand wildcards in args before command execution</li>
     *   <li>"Did you mean?": suggest similar command on command-not-found</li>
     * </ul>
     *
     * @param input raw command string
     * @return the {@link CommandResult} of the final executed segment, or a
     *         zero-exit success result if the input was blank / produced no segments
     */
    private CommandResult runPlan(String input) {
        ShellParser.ParsedPlan plan = new ShellParser().parse(input);

        // Checking whether structure is invariant from the parser
        assert plan.operators.size() == Math.max(0, plan.segments.size() - 1)
                : "ParsedPlan invariant violated: operators=" + plan.operators.size()
                + " segments=" + plan.segments.size();

        // When nothing to execute
        if (plan.segments.isEmpty()) {
            LOGGER.fine("runPlan: no segments to execute");
            return CommandResult.success("");
        }

        CommandResult lastResult = CommandResult.success("");
        String pipedStdin = null; // stdout carried forward through a pipe

        for (int i = 0; i < plan.segments.size(); i++) {
            ShellParser.Segment segment = plan.segments.get(i);

            assert segment != null : "Null segment at index " + i;
            assert segment.commandName != null && !segment.commandName.isBlank()
                    : "Segment at index " + i + " has blank commandName";

            // ── v1.0: Check the operator that precedes this segment ──
            // operators.get(i-1) sits between segment[i-1] and segment[i]
            if (i > 0) {
                ShellParser.TokenType precedingOp = plan.operators.get(i - 1);

                if (precedingOp == ShellParser.TokenType.AND && lastExitCode != 0) {
                    // the last command failed so skipping the next command
                    // && requires the previous command to have succeeded
                    break;
                }

                // TODO v2.0 (Owner A): handle OR operator
                // if (precedingOp == ShellParser.TokenType.OR && lastExitCode == 0) {
                //     break;
                // }

                if (precedingOp != ShellParser.TokenType.PIPE) {
                    // SEMICOLON or AND (that passed): clear any leftover piped stdin
                    pipedStdin = null;
                }
                // PIPE: pipedStdin was already set at the end of the previous iteration
            }

            // pipedStdin is non-null only when the preceding operator was PIPE
            String stdin = pipedStdin;
            pipedStdin = null;

            // TODO v2.0 (Owner A): handle input redirect (< operator)
            // if (segment.inputRedirect != null && !segment.inputRedirect.isEmpty()) {
            //     stdin = vfs.readFile(segment.inputRedirect, workingDir);
            // }

            // TODO v2.0 (Owner A): resolve alias before registry lookup
            // String resolvedName = segment.commandName;
            // if (aliases.containsKey(resolvedName)) {
            //     resolvedName = aliases.get(resolvedName);
            // }

            // TODO v2.0 (Owner B): expand glob patterns in arguments
            // String[] expandedArgs = expandGlobs(segment.args);

            // ── v1.0: Look up command in registry ──
            Command command = registry.get(segment.commandName);
            if (command == null) {
                String errorMsg = segment.commandName + ": command not found";
                LOGGER.log(Level.WARNING, "Command not found: ''{0}''", segment.commandName);
                // TODO v2.0 (Owner B): add "Did you mean?" suggestion
                // String suggestion = suggestCommand(segment.commandName);
                // if (suggestion != null) { errorMsg += "\n" + suggestion; }
                ui.println(errorMsg);
                setLastExitCode(127);
                lastResult = CommandResult.error(errorMsg);
                continue; // no piped output from a missing command
            }

            // ── v1.0: Execute the command ──
            CommandResult result = command.execute(this, segment.args, stdin);

            // Print stderr immediately (user is not redirected)
            if (!result.getStderr().isEmpty()) {
                ui.println(result.getStderr());
            }

            // Handle output redirect (> or >>)
            if (segment.redirect != null) {
                // Flush stdout to the target file; suppress it from terminal / pipe
                vfs.writeFile(
                        segment.redirect.target,
                        workingDir,
                        result.getStdout(),
                        segment.redirect.isAppend()
                );
                // stdout consumed by redirect, replaced with an empty success so
                // nothing gets printed or forwarded downstream
                result = CommandResult.success("");
            }

            // Carry stdout forward if the next operator is PIPE
            boolean nextIsPipe = (i < plan.operators.size())
                    && plan.operators.get(i) == ShellParser.TokenType.PIPE;
            if (nextIsPipe) {
                pipedStdin = result.getStdout();
            }

            // Update session state
            setLastExitCode(result.getExitCode());
            lastResult = result;

            if (result.shouldExit()) {
                running = false;
                break;
            }
        }

        return lastResult;
    }

    // Getters / Setters

    public VirtualFileSystem getVfs() {
        return vfs;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String path) {
        this.workingDir = path;
    }

    public String getPreviousDir() {
        return previousDir;
    }

    public void setPreviousDir(String dir) {
        this.previousDir = dir;
    }

    public int getLastExitCode() {
        return lastExitCode;
    }

    public void setLastExitCode(int code) {
        this.lastExitCode = code;
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    public Ui getUi() {
        return ui;
    }

    public String getPrompt() {
        return "user@linuxlingo:" + workingDir + "$ ";
    }

    public void replaceVfs(VirtualFileSystem newVfs) {
        this.vfs = newVfs;
    }

    public boolean isRunning() {
        return running;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public Object getLineReader() {
        return lineReader;
    }

    public void setLineReader(Object reader) {
        this.lineReader = reader;
    }

    /**
     * Get the in-memory command history list.
     *
     * @return mutable list of command history entries
     */
    public List<String> getCommandHistory() {
        return commandHistory;
    }

    // ─── "Did you mean?" suggestion ─────────────────────────────

    /**
     * Suggest a similar command name using edit distance.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
     * <p>Should iterate all registered command names, compute edit distance,
     * and return "Did you mean 'X'?" if the best match has distance ≤ 2.</p>
     *
     * @param input the unrecognized command name
     * @return a suggestion string like "Did you mean 'ls'?" or null
     */
    public String suggestCommand(String input) {
        // TODO v2.0 (Owner B): implement using editDistance() and registry.getAllNames()
        return null;
    }

    /**
     * Compute Levenshtein edit distance between two strings.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
     * <p>Use dynamic programming: dp[i][j] = min edits to transform a[0..i-1] to b[0..j-1].</p>
     *
     * @param a first string
     * @param b second string
     * @return the edit distance
     */
    static int editDistance(String a, String b) {
        // TODO v2.0 (Owner B): implement Levenshtein DP algorithm
        return Math.abs(a.length() - b.length()); // placeholder
    }

    // ─── Glob expansion ─────────────────────────────────────────

    /**
     * Expand glob patterns (*, ?) in arguments against the VFS.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
     * <p>For each arg containing wildcards and a path separator,
     * expand against VFS using {@link #expandSingleGlob(String)}.
     * If no matches, keep the literal arg.</p>
     *
     * @param args the original arguments
     * @return expanded arguments (or originals if no globs matched)
     */
    public String[] expandGlobs(String[] args) {
        // TODO v2.0 (Owner B): implement glob expansion
        return args; // placeholder: return args unchanged
    }

    /**
     * Expand a single glob pattern against the VFS.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
     * <p>Split the pattern at the last '/', use the prefix as the directory
     * and the suffix as the file pattern. Use
     * {@link VirtualFileSystem#findByName(String, String, String)} for matching.</p>
     *
     * @param pattern a glob pattern like "/home/user/*.txt"
     * @return list of matched absolute paths, or empty if no matches
     */
    private List<String> expandSingleGlob(String pattern) {
        // TODO v2.0 (Owner B): implement VFS glob matching
        return new ArrayList<>(); // placeholder
    }
}
