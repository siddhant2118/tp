package linuxlingo.shell.command;

import java.util.Map;
import java.util.logging.Logger;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;


/**
 * Removes one or more shell aliases.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code unalias name [name ...]} — removes the named alias(es).</li>
 *   <li>{@code unalias -a}              — removes all defined aliases.</li>
 * </ul>
 *
 * <p>Returns an error for each alias name that does not exist.
 * All valid names in the same invocation are still removed.</p>
 */
public class UnaliasCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(UnaliasCommand.class.getName());

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            return CommandResult.error("unalias: usage:" + getUsage());
        }

        Map<String, String> aliases = session.getAliases();

        // -a flag clears all aliases
        if (args[0].equals("-a")) {
            if (args.length > 1) {
                return CommandResult.error("unalias: -a: cannot be used with alias names");
            }
            aliases.clear();
            LOGGER.fine("All aliases cleared");
            return CommandResult.success("");
        }

        return removeNamedAliases(aliases, args);
    }

    /**
     * Removes each named alias from the map.
     * Collects errors for names that were not found, but still removes all valid names.
     *
     * @param aliases the live alias map from the session
     * @param names   the alias names to remove
     * @return success if all names were found; otherwise an error listing missing names
     */
    private CommandResult removeNamedAliases(Map<String, String> aliases, String[] names) {
        // checking if '-a' appears anywhere (raises error if it is not the first arg)
        for (String name : names) {
            if (name.startsWith("-")) {
                return CommandResult.error("unalias: " + name + ": invalid option");
            }
        }

        StringBuilder errors = new StringBuilder();

        for (String name: names) {
            if (aliases.remove(name) == null) {
                // Map.remove returns null when the key is absent
                if (!errors.isEmpty()) {
                    errors.append('\n');
                }
                errors.append("unalias: ").append(name).append(": not found");
                LOGGER.fine(() -> "unalias: alias not found: " + name);
            } else {
                LOGGER.fine(() -> "Alias removed: " + name);
            }
        }

        return !errors.isEmpty() ? CommandResult.error(errors.toString()) : CommandResult.success("");
    }

    @Override
    public String getUsage() {
        return "unalias [-a] <name> [name ...]";
    }

    @Override
    public String getDescription() {
        return "Remove shell aliases";
    }
}
