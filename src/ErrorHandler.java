import java.util.*;

public class ErrorHandler {

    // ── Error categories ──────────────────────────────────────────────────────
    public enum ErrorType {
        INVALID_CHARACTER,
        MALFORMED_FLOAT,
        UNTERMINATED_STRING,
        UNTERMINATED_CHAR,
        INVALID_IDENTIFIER,
        IDENTIFIER_TOO_LONG,
        UNCLOSED_COMMENT
    }

    // ── One error record ──────────────────────────────────────────────────────
    public static class ScanError {
        private final ErrorType type;
        private final int       line;
        private final int       column;
        private final String    lexeme;
        private final String    reason;

        public ScanError(ErrorType type, int line, int column,
                         String lexeme, String reason) {
            this.type   = type;
            this.line   = line;
            this.column = column;
            this.lexeme = lexeme;
            this.reason = reason;
        }

        public ErrorType getType()   { return type;   }
        public int       getLine()   { return line;   }
        public int       getColumn() { return column; }
        public String    getLexeme() { return lexeme; }
        public String    getReason() { return reason; }

        /** Format: [ERROR_TYPE] Line X, Col Y  |  lexeme: "..."  |  reason */
        @Override
        public String toString() {
            return String.format("[%-22s] Line: %-4d Col: %-4d | lexeme: %-20s | %s",
                    type, line, column,
                    "\"" + lexeme + "\"",
                    reason);
        }
    }

    // ── Storage ───────────────────────────────────────────────────────────────
    private final List<ScanError> errors = new ArrayList<>();

    // ── Record helpers ────────────────────────────────────────────────────────
    public void report(ErrorType type, int line, int column,
                       String lexeme, String reason) {
        errors.add(new ScanError(type, line, column, lexeme, reason));
    }

    public boolean hasErrors()          { return !errors.isEmpty(); }
    public int     errorCount()         { return errors.size();     }
    public List<ScanError> getErrors()  { return Collections.unmodifiableList(errors); }

    // ── Detection methods (called by ManualScanner / JFlex action code) ───────

    /**
     * Detect invalid character (@, $, etc.)
     * Recovery: skip the character and continue.
     */
    public void detectInvalidChar(char ch, int line, int col) {
        report(ErrorType.INVALID_CHARACTER, line, col,
               String.valueOf(ch),
               "Character '" + ch + "' is not part of the language alphabet");
    }

    /**
     * Detect malformed floating-point literal (e.g. 3.1234567 — too many decimals).
     * Recovery: consume the valid prefix as a float and treat the rest as a new token.
     */
    public void detectMalformedFloat(String lexeme, int line, int col) {
        report(ErrorType.MALFORMED_FLOAT, line, col, lexeme,
               "Floating-point literal has more than 6 fractional digits");
    }

    /**
     * Detect unterminated string literal.
     * Recovery: skip to the next newline and continue.
     */
    public void detectUnterminatedString(String partial, int line, int col) {
        report(ErrorType.UNTERMINATED_STRING, line, col, partial,
               "String literal not closed before end of line");
    }

    /**
     * Detect unterminated character literal.
     * Recovery: skip to the closing quote or newline and continue.
     */
    public void detectUnterminatedChar(String partial, int line, int col) {
        report(ErrorType.UNTERMINATED_CHAR, line, col, partial,
               "Character literal not properly closed");
    }

    /**
     * Detect invalid identifier (starts with lowercase, digit, or underscore).
     * Recovery: emit an ERROR token per bad character and continue.
     */
    public void detectInvalidIdentifier(String lexeme, int line, int col) {
        String reason;
        if (lexeme.isEmpty()) {
            reason = "Empty identifier";
        } else if (Character.isLowerCase(lexeme.charAt(0))) {
            reason = "Identifier must start with an uppercase letter [A-Z]; "
                   + "found lowercase '" + lexeme.charAt(0) + "'";
        } else if (Character.isDigit(lexeme.charAt(0))) {
            reason = "Identifier must start with an uppercase letter [A-Z]; "
                   + "found digit '" + lexeme.charAt(0) + "'";
        } else if (lexeme.charAt(0) == '_') {
            reason = "Identifier must start with an uppercase letter [A-Z]; "
                   + "found underscore";
        } else {
            reason = "Invalid identifier format";
        }
        report(ErrorType.INVALID_IDENTIFIER, line, col, lexeme, reason);
    }

    /**
     * Detect identifier that exceeds the 31-character limit.
     * Recovery: truncate to 31 characters and continue.
     */
    public void detectIdentifierTooLong(String lexeme, int line, int col) {
        report(ErrorType.IDENTIFIER_TOO_LONG, line, col,
               lexeme.substring(0, Math.min(lexeme.length(), 40)) + "...",
               "Identifier exceeds maximum length of 31 characters "
               + "(length: " + lexeme.length() + ")");
    }

    /**
     * Detect unclosed multi-line comment.
     * Recovery: treat rest of file as comment body and continue.
     */
    public void detectUnclosedComment(String partial, int line, int col) {
        String preview = partial.length() > 30
                ? partial.substring(0, 30) + "..."
                : partial;
        report(ErrorType.UNCLOSED_COMMENT, line, col, preview,
               "Multi-line comment opened with #* but never closed with *#");
    }

    // ── Display ───────────────────────────────────────────────────────────────

    /** Print all errors to stdout. */
    public void displayErrors() {
        System.out.println("\n=== ERROR REPORT (Part 3) ===");
        if (errors.isEmpty()) {
            System.out.println("No errors found.");
            return;
        }

        System.out.printf("Total errors: %d%n", errors.size());
        System.out.println("-".repeat(90));

        // Group by type for readability
        Map<ErrorType, List<ScanError>> grouped = new LinkedHashMap<>();
        for (ErrorType t : ErrorType.values()) grouped.put(t, new ArrayList<>());
        for (ScanError e : errors) grouped.get(e.getType()).add(e);

        for (ErrorType type : ErrorType.values()) {
            List<ScanError> group = grouped.get(type);
            if (group.isEmpty()) continue;

            System.out.printf("%n  ▶ %s (%d)%n", type, group.size());
            for (ScanError e : group) {
                System.out.println("    " + e);
            }
        }

        System.out.println("\n" + "-".repeat(90));
        System.out.printf("Error recovery: scanner continued after each error "
                        + "and reported all %d error(s).%n", errors.size());
    }

    /** Summary line — how many errors of each type. */
    public void displaySummary() {
        if (errors.isEmpty()) { System.out.println("Errors: 0"); return; }
        Map<ErrorType, Long> counts = new LinkedHashMap<>();
        for (ScanError e : errors)
            counts.merge(e.getType(), 1L, Long::sum);
        System.out.println("\n=== ERROR SUMMARY ===");
        counts.forEach((t, c) -> System.out.printf("  %-25s : %d%n", t, c));
        System.out.printf("  %-25s : %d%n", "TOTAL", errors.size());
    }
}