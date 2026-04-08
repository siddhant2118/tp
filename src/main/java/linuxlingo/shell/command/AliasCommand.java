package linuxlingo.shell.command;

import java.util.Map;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Creates or displays shell aliases.
 * Syntax: {@code alias [name=value]}.
 *
 * <p>Supports three modes:
 * list all aliases, print one alias by name, or define/update an alias using
 * {@code name=value}. Single quotes around values are stripped.</p>
 */
public class AliasCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            return listAliases(session);
        }

        // name without = : show that specific alias
        if (!args[0].contains("=")) {
            return showAlias(session, args[0]);
        }

        return setAlias(session, args[0]);
    }

    /**
     * Prints all aliases in shell-compatible syntax.
     *
     * @param session active shell session
     * @return alias listing, or empty output when none exist
     */
    private CommandResult listAliases(ShellSession session) {
        Map<String, String > aliases = session.getAliases();
        if (aliases.isEmpty()) {
            return CommandResult.success("");
        }

        StringBuilder sbuild = new StringBuilder();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (sbuild.length() > 0) {
                sbuild.append('\n');
            }
            sbuild.append("alias ").append(entry.getKey()).append("='").append(entry.getValue()).append("'");
        }
        return CommandResult.success(sbuild.toString());
    }

    /**
     * Parses an alias definition and stores it in session aliases.
     *
     * @param session active shell session
     * @param definition the raw alias definition argument
     * @return success when stored, or an error for invalid definitions
     */
    private CommandResult setAlias(ShellSession session, String definition) {
        int eqIndex = definition.indexOf('=');
        if (eqIndex <= 0) {
            return CommandResult.error("alias: invalid format: '" + definition + "' (expected name=value)");
        }

        String name = definition.substring(0, eqIndex);
        String value = definition.substring(eqIndex + 1);
        value = stripSurroundingQuotes(value);

        if (name.isBlank()) {
            return CommandResult.error("alias: name must not be blank");
        }

        session.getAliases().put(name, value);
        return CommandResult.success("");
    }

    /**
     * Removes a single pair of surrounding single quotes from a value.
     *
     * @param s raw alias value
     * @return unquoted value when quoted, else the original string
     */
    private String stripSurroundingQuotes(String s) {
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Prints a specific alias by name.
     *
     * @param session active shell session
     * @param name alias key
     * @return alias output or not-found error
     */
    private CommandResult showAlias(ShellSession session, String name) {
        String value = session.getAliases().get(name);
        if (value == null) {
            return CommandResult.error("alias: " + name + ": not found");
        }
        return CommandResult.success("alias " + name + "='" + value + "'");
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
