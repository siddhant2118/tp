package linuxlingo.shell.command;

import java.util.Map;
import java.util.logging.Logger;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Creates or displays shell aliases.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code alias}            — lists all currently defined aliases.</li>
 *   <li>{@code alias name=value} — defines a new alias, stripping surrounding quotes.</li>
 *   <li>{@code alias name}       — displays the value of a specific alias.</li>
 * </ul>
 */
public class AliasCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(AliasCommand.class.getName());

    private static final String ALIAS_FORMAT = "alias %s='%s'";

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            return listAliases(session);
        }

        String primaryArg = args[0];

        // if name is provided without '=' then show that specific alias
        if (!primaryArg.contains("=")) {
            return showAlias(session, primaryArg);
        }

        return setAlias(session, primaryArg);
    }

    /**
     * Lists all currently defined aliases, one per line in {@code alias name='value'} format.
     *
     * @param session the active shell session
     * @return a {@link CommandResult} containing the formatted alias list, or empty if none defined
     */
    private CommandResult listAliases(ShellSession session) {
        Map<String, String> aliases = session.getAliases();
        if (aliases.isEmpty()) {
            return CommandResult.success("");
        }

        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (!output.isEmpty()) {
                output.append('\n');
            }
            output.append(String.format(ALIAS_FORMAT, entry.getKey(), entry.getValue()));
        }
        return CommandResult.success(output.toString());
    }

    /**
     * Parses a {@code name=value} definition and stores it in the session's alias map.
     * Surrounding single or double quotes are stripped from the value.
     *
     * @param session    the active shell session
     * @param definition the raw alias definition (e.g. {@code ll=ls -la} or {@code ll='ls -la'})
     * @return a {@link CommandResult} indicating success or a descriptive error
     */
    private CommandResult setAlias(ShellSession session, String definition) {
        int eqIndex = definition.indexOf('=');

        // eqIndex == 0 means the name portion is empty (e.g. "=value")
        // eqIndex < 0 means no '=' present at all, but setAlias() is only
        // called when args[0].contains("="), so < 0 is defensive only
        if (eqIndex <= 0) {
            return CommandResult.error("alias: invalid format: '" + definition + "' (expected name=value)");
        }

        String name = definition.substring(0, eqIndex);
        String value = stripSurroundingQuotes(definition.substring(eqIndex + 1));

        if (name.isBlank()) {
            return CommandResult.error("alias: name must not be blank");
        }

        session.getAliases().put(name, value);
        LOGGER.fine(() -> "Alias set: " + name + " -> " + value);
        return CommandResult.success("");
    }

    /**
     * Strips a matching pair of surrounding single or double quotes from {@code s}.
     * If {@code s} is not surrounded by matching quotes, it is returned unchanged.
     *
     * @param s the string to strip
     * @return the unquoted string
     */
    private String stripSurroundingQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt((s.length() - 1));
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * Displays the value of a single named alias.
     *
     * @param session the active shell session
     * @param name    the alias name to look up
     * @return a {@link CommandResult} with the alias definition, or an error if not found
     */
    private CommandResult showAlias(ShellSession session, String name) {
        String value = session.getAliases().get(name);
        if (value == null) {
            return CommandResult.error("alias: " + name + ": not found");
        }
        return CommandResult.success(String.format(ALIAS_FORMAT, name, value));
    }

    @Override
    public String getUsage() {
        return "alias [name=value]";
    }

    @Override
    public String getDescription() {
        return "Create or display shell aliases";
    }
}
