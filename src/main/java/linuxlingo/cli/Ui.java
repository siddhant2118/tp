package linuxlingo.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Provides all user-facing I/O for LinuxLingo.
 *
 * <p>Acts as a single point of contact for reading input from the user
 * and printing output or error messages. All UI interactions should go
 * through this class to allow easy redirection in tests.</p>
 */
public class Ui {
    private final Scanner scanner;
    private final PrintStream out;
    private final PrintStream err;

    /**
     * Constructs a Ui using standard input, output, and error streams.
     */
    public Ui() {
        this(System.in, System.out, System.err);
    }

    /**
     * Constructs a Ui with custom input and output streams.
     * The error stream defaults to the same as the output stream.
     *
     * @param in  the input stream to read from.
     * @param out the output stream to write to.
     */
    public Ui(InputStream in, PrintStream out) {
        this(in, out, out);
    }

    /**
     * Constructs a Ui with fully custom input, output, and error streams.
     *
     * @param in  the input stream to read from.
     * @param out the output stream for normal output.
     * @param err the output stream for error messages.
     */
    public Ui(InputStream in, PrintStream out, PrintStream err) {
        this.scanner = new Scanner(in);
        this.out = out;
        this.err = err;
    }

    /**
     * Reads a line from the input stream without printing a prompt.
     *
     * @return the next line of input, or {@code null} if the stream is exhausted.
     */
    public String readLine() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return null;
    }

    /**
     * Prints a prompt string and then reads a line from the input stream.
     *
     * @param prompt the prompt to display before reading.
     * @return the next line of input, or {@code null} if the stream is exhausted.
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        return readLine();
    }

    /**
     * Prints a message to the output stream without a trailing newline.
     *
     * @param message the message to print.
     */
    public void print(String message) {
        out.print(message);
        out.flush();
    }

    /**
     * Prints a message followed by a newline to the output stream.
     *
     * @param message the message to print.
     */
    public void println(String message) {
        out.println(message);
    }

    /**
     * Prints an error message to the error stream.
     *
     * @param message the error message to print.
     */
    public void printError(String message) {
        err.println(message);
    }

    /**
     * Prints the LinuxLingo ASCII-art welcome banner and a help hint.
     */
    public void printWelcome() {
        println("");
        println(" _     _                  _     _");
        println("| |   (_)_ __  _   ___  _| |   (_)_ __   __ _  ___");
        println("| |   | | '_ \\| | | \\ \\/ / |   | | '_ \\ / _` |/ _ \\");
        println("| |___| | | | | |_| |>  <| |___| | | | | (_| | (_) |");
        println("|_____|_|_| |_|\\__,_/_/\\_\\_____|_|_| |_|\\__, |\\___/");
        println("                                         |___/");
        println("");
        println("Welcome to LinuxLingo! Type 'help' for available commands.");
        println("");
    }

    /**
     * Clears the terminal screen using ANSI escape codes.
     */
    public void clearScreen() {
        out.print("\033[H\033[2J");
        out.flush();
    }

    /**
     * Prompts the user for a yes/no confirmation.
     *
     * @param prompt the confirmation question to display.
     * @return {@code true} if the user responds with "y" or "yes" (case-insensitive).
     */
    public boolean confirm(String prompt) {
        String input = readLine(prompt + " (y/n): ");
        return input != null && (input.trim().equalsIgnoreCase("y") || input.trim().equalsIgnoreCase("yes"));
    }
}
