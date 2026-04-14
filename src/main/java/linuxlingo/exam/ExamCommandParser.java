package linuxlingo.exam;

import java.util.HashSet;
import java.util.Set;

/**
 * Strict parser for {@code exam} command flags.
 *
 * <p>Supported formats:
 * <ul>
 *   <li>{@code exam} (interactive)</li>
 *   <li>{@code exam -t TOPIC [-n COUNT] [-random]}</li>
 *   <li>{@code exam -topics}</li>
 *   <li>{@code exam -random} (one random question)</li>
 * </ul>
 *
 * <p>This parser rejects unknown flags, duplicate flags, and missing required values.
 */
public final class ExamCommandParser {

    /** Usage string shown on parsing errors. */
    public static final String USAGE = String.join(System.lineSeparator(),
            "Usage:",
            "  exam                         Start an exam (interactive topic selection)",
            "  exam -t TOPIC [-n COUNT] [-random]",
            "  exam -topics                 List all available exam topics",
            "  exam -random                 One random question, then return");

    private ExamCommandParser() {
        // utility
    }


    /** Parsed result for an exam command, or an error with a message+usage. */
    public static final class ParsedExamArgs {
        public final String topic;
        public final Integer count;
        public final boolean random;
        public final boolean listTopics;

        public final boolean ok;
        public final String errorMessage;

        private ParsedExamArgs(String topic, Integer count, boolean random, boolean listTopics,
                               boolean ok, String errorMessage) {
            this.topic = topic;
            this.count = count;
            this.random = random;
            this.listTopics = listTopics;
            this.ok = ok;
            this.errorMessage = errorMessage;
        }

        public static ParsedExamArgs success(String topic, Integer count, boolean random, boolean listTopics) {
            return new ParsedExamArgs(topic, count, random, listTopics, true, null);
        }

        public static ParsedExamArgs error(String message) {
            return new ParsedExamArgs(null, null, false, false, false, message);
        }
    }


    /**
     * Parse tokens that come after the {@code exam} command.
     *
     * @param args tokens after {@code exam} (may be empty)
     */
    public static ParsedExamArgs parse(String[] args) {
        String topic = null;
        Integer count = null;
        boolean random = false;
        boolean listTopics = false;

        Set<String> seen = new HashSet<>();

        for (int i = 0; i < args.length; i++) {
            // Be defensive: callers may pass tokens with extra whitespace.
            String token = args[i] == null ? "" : args[i].trim();
            switch (token) {
            case "-t" -> {
                if (!seen.add("-t")) {
                    return ParsedExamArgs.error("Duplicate flag: -t");
                }
                String value = requireValue(args, i);
                if (value == null) {
                    return ParsedExamArgs.error("Missing value for -t");
                }
                topic = value;
                i++; // consume value
            }
            case "-n" -> {
                if (!seen.add("-n")) {
                    return ParsedExamArgs.error("Duplicate flag: -n");
                }
                String value = requireValue(args, i);
                if (value == null) {
                    return ParsedExamArgs.error("Missing value for -n");
                }
                try {
                    count = Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    // Handle decimals like "1.5" by truncating
                    try {
                        count = (int) Double.parseDouble(value.trim());
                    } catch (NumberFormatException e2) {
                        return ParsedExamArgs.error("Invalid count: " + value);
                    }
                }
                i++; // consume value
            }
            case "-random" -> {
                if (!seen.add("-random")) {
                    return ParsedExamArgs.error("Duplicate flag: -random");
                }
                random = true;
            }
            case "-topics" -> {
                if (!seen.add("-topics")) {
                    return ParsedExamArgs.error("Duplicate flag: -topics");
                }
                listTopics = true;
            }
            default -> {
                if (token.startsWith("-")) {
                    return ParsedExamArgs.error("Unknown flag: " + token);
                }
                return ParsedExamArgs.error("Unexpected argument: " + token);
            }
            }
        }

        if (listTopics && (topic != null || count != null || random)) {
            return ParsedExamArgs.error("-topics cannot be combined with other flags");
        }

        // Spec: exam -t TOPIC [-n COUNT] [-random]
        // If -n is provided, topic is required (otherwise behaviour is ambiguous).
        if (count != null && topic == null) {
            return ParsedExamArgs.error("-n requires -t TOPIC");
        }

        return ParsedExamArgs.success(topic, count, random, listTopics);
    }

    private static String requireValue(String[] args, int flagIndex) {
        int valueIndex = flagIndex + 1;
        if (valueIndex >= args.length) {
            return null;
        }
        String value = args[valueIndex];
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        // Allow negative numbers (e.g. "-1") but reject flags (e.g. "-t", "-random")
        if (value.startsWith("-") && !value.matches("-\\d+")) {
            return null;
        }
        return value;
    }
}

