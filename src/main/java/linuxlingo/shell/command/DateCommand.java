package linuxlingo.shell.command;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints the current date and time.
 * Syntax: {@code date [+FORMAT]}.
 *
 * <p>Without arguments, this command uses the default format
 * {@code EEE MMM dd HH:mm:ss yyyy}. If {@code +FORMAT} is provided,
 * it accepts either Java {@link java.time.format.DateTimeFormatter} syntax
 * or a subset of {@code strftime} tokens (for example, {@code +%Y-%m-%d}).</p>
 */
public class DateCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        String format = "EEE MMM dd HH:mm:ss yyyy";
        if (args.length > 0 && args[0].startsWith("+")) {
            String rawFormat = args[0].substring(1);
            // Handle %s (Unix epoch seconds) specially — it cannot be
            // expressed as a DateTimeFormatter pattern.
            if (rawFormat.equals("%s")) {
                return CommandResult.success(String.valueOf(Instant.now().getEpochSecond()));
            }
            format = convertStrftimeToJava(rawFormat);
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            // Use ZonedDateTime so timezone-related specifiers (%Z → 'z') work.
            return CommandResult.success(ZonedDateTime.now().format(formatter));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("date: invalid date format");
        }
    }

    /**
     * Converts a Linux strftime format string to a Java DateTimeFormatter pattern.
     * If the format contains '%' specifiers, they are converted. Otherwise,
     * the format is returned as-is and treated as a Java formatter pattern.
     */
    private String convertStrftimeToJava(String format) {
        if (!format.contains("%")) {
            return format;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '%' && i + 1 < format.length()) {
                i++;
                char spec = format.charAt(i);
                switch (spec) {
                case 'Y':
                    sb.append("yyyy");
                    break;
                case 'm':
                    sb.append("MM");
                    break;
                case 'd':
                    sb.append("dd");
                    break;
                case 'H':
                    sb.append("HH");
                    break;
                case 'M':
                    sb.append("mm");
                    break;
                case 'S':
                    sb.append("ss");
                    break;
                case 'A':
                    sb.append("EEEE");
                    break;
                case 'a':
                    sb.append("EEE");
                    break;
                case 'B':
                    sb.append("MMMM");
                    break;
                case 'b':
                    sb.append("MMM");
                    break;
                case 'p':
                    sb.append("a");
                    break;
                case 'I':
                    sb.append("hh");
                    break;
                case 'j':
                    sb.append("DDD");
                    break;
                case 'Z':
                    sb.append("z");
                    break;
                case 'n':
                    sb.append("\n");
                    break;
                case 't':
                    sb.append("\t");
                    break;
                case '%':
                    sb.append("'%'");
                    break;
                default:
                    // Unknown specifier — keep literal %X by quoting both chars
                    sb.append("'%").append(spec).append("'");
                    break;
                }
            } else {
                // Literal characters need to be quoted for DateTimeFormatter
                if (Character.isLetter(c)) {
                    sb.append('\'').append(c).append('\'');
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String getUsage() {
        return "date";
    }

    @Override
    public String getDescription() {
        return "Print the current date and time";
    }
}
