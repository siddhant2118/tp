package linuxlingo.shell;

import java.util.List;

/**
 * Transforms a raw input string into a structured execution plan.
 *
 * <p><b>Owner: A</b></p>
 *
 * <p>Parsing pipeline:</p>
 * <pre>
 *   Raw Input String
 *       │
 *       ▼
 *   Tokenizer        — split by whitespace, respecting quotes ("..." and '...')
 *       │
 *       ▼
 *   Operator Splitter — split on operators: |  &gt;  &gt;&gt;  &amp;&amp;  ;
 *       │
 *       ▼
 *   Command Segment List — each segment = command name + arguments
 *       │
 *       ▼
 *   Execution Engine  — execute segments, connecting via pipes/redirects
 * </pre>
 */
public class ShellParser {

    public enum TokenType {
        WORD, PIPE, REDIRECT, APPEND, AND, SEMICOLON
    }

    public static class Token {
        public final String value;
        public final TokenType type;

        public Token(String value, TokenType type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return type + ":" + value;
        }
    }

    public static class RedirectInfo {
        public final String operator; // ">" or ">>"
        public final String target;   // file path

        public RedirectInfo(String operator, String target) {
            this.operator = operator;
            this.target = target;
        }

        public boolean isAppend() {
            return ">>".equals(operator);
        }
    }

    public static class Segment {
        public final String commandName;
        public final String[] args;
        public final RedirectInfo redirect;

        public Segment(String commandName, String[] args, RedirectInfo redirect) {
            this.commandName = commandName;
            this.args = args;
            this.redirect = redirect;
        }
    }

    public static class ParsedPlan {
        public final List<Segment> segments;
        public final List<TokenType> operators;

        public ParsedPlan(List<Segment> segments, List<TokenType> operators) {
            this.segments = segments;
            this.operators = operators;
        }
    }

    /**
     * Parse a raw input string into a {@link ParsedPlan}.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Tokenize input — split by whitespace, respecting single/double quotes.
     *       Recognize operators: {@code |}, {@code >}, {@code >>}, {@code &&}, {@code ;}.</li>
     *   <li>Split token list by inter-segment operators (PIPE, AND, SEMICOLON).</li>
     *   <li>Within each segment, extract command name, args, and optional redirect info.</li>
     * </ol>
     */
    public ParsedPlan parse(String input) {
        List<Segment> segments = new java.util.ArrayList<>();
        List<TokenType> operators = new java.util.ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            return new ParsedPlan(segments, operators);
        }

        String trimmed = input.trim();

        if (trimmed.contains(" | ")) {
            String[] pipeParts = trimmed.split(" \\| ");
            for (int i = 0; i < pipeParts.length; i++) {
                segments.add(parseSegment(pipeParts[i].trim()));
                if (i <pipeParts.length - 1) {
                    operators.add(TokenType.PIPE);
                }
            }
            return new ParsedPlan(segments, operators);
        }

        if (trimmed.contains( " && ")) {
            String[] andParts = trimmed.split(" && ");
            for (int i = 0; i < andParts.length; i++) {
                segments.add(parseSegment(andParts[i].trim()));
                if (i < andParts.length - 1) {
                    operators.add(TokenType.AND);
                }
            }
            return new ParsedPlan(segments, operators);
        }

        segments.add(parseSegment(trimmed));
        return new ParsedPlan(segments, operators);
    }

    private Segment parseSegment(String segmentStr) {
        String[] parts = segmentStr.split("\\s+");
        String commandName = parts[0];
        String[] args = new String[Math.max(0, parts.length - 1)];

        for (int i = 1; i < parts.length; i ++) {
            args[i - 1] = parts[i];
        }

        return new Segment(commandName, args, null);
    }
}
