package linuxlingo.shell.utility;

/**
 * Exit codes used across the shell.
 *
 * <p>Centralising these prevents magic numbers from being scattered
 * across command implementations and the session engine.</p>
 */
public class ExitCodes {

    /** General runtime error */
    public static final int GENERAL_ERROR = 1;

    /** Shell syntax or usage error. */
    public static final int SYNTAX_ERROR = 2;

    /** Command not found. */
    public static final int COMMAND_NOT_FOUND = 127;

    // Prevent instantiation, this is a constants class
    private ExitCodes() {
        throw new AssertionError("ExitCodes is not instantiable");
    }
}
