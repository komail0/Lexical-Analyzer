import java.io.*;
import java.util.*;


public class ManualScanner {

    private String       input;
    private int          position;
    private int          line;
    private int          column;
    private int          whitespaceRemoved;
    private SymbolTable  symbolTable;
    private ErrorHandler errorHandler;      // Part 3

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "finish", "loop", "condition", "declare", "output",
            "input", "function", "return", "break", "continue", "else"
    ));
    private static final Set<String> BOOLEANS = new HashSet<>(Arrays.asList("true", "false"));

    public ManualScanner(String input) {
        this.input             = input;
        this.position          = 0;
        this.line              = 1;
        this.column            = 1;
        this.whitespaceRemoved = 0;
        this.symbolTable       = new SymbolTable();
        this.errorHandler      = new ErrorHandler();
    }

    public int          getWhitespaceRemoved() { return whitespaceRemoved; }
    public int          getCurrentLine()        { return line;             }
    public SymbolTable  getSymbolTable()        { return symbolTable;      }
    public ErrorHandler getErrorHandler()       { return errorHandler;     }

    public List<Token> scanAll() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = getNextToken()) != null) tokens.add(token);
        return tokens;
    }

    // ── Core ──────────────────────────────────────────────────────────────────
    public Token getNextToken() {
        if (position >= input.length()) return null;

        int   startLine = line;
        int   startCol  = column;
        Token token;

        if ((token = tryMultiLineComment(startLine, startCol))   != null) return token;
        if ((token = trySingleLineComment(startLine, startCol))  != null) return token;
        if ((token = tryMultiCharOperator(startLine, startCol))  != null) return token;
        if ((token = tryKeyword(startLine, startCol))            != null) return token;
        if ((token = tryBoolean(startLine, startCol))            != null) return token;
        if ((token = tryIdentifier(startLine, startCol))         != null) return token;
        if ((token = tryFloatingPoint(startLine, startCol))      != null) return token;
        if ((token = tryInteger(startLine, startCol))            != null) return token;
        if ((token = tryString(startLine, startCol))             != null) return token;
        if ((token = tryCharacter(startLine, startCol))          != null) return token;
        if ((token = trySingleCharOperator(startLine, startCol)) != null) return token;
        if ((token = tryPunctuator(startLine, startCol))         != null) return token;
        if ((token = tryWhitespace(startLine, startCol))         != null) return token;

        // ── Error Recovery (Part 3): invalid character ──────────────────────
        // Report the error, skip one character, and CONTINUE scanning.
        char ch = peek();
        advance();
        errorHandler.detectInvalidChar(ch, startLine, startCol);
        return new Token(TokenType.ERROR, String.valueOf(ch), startLine, startCol);
    }

    // ── Comments ──────────────────────────────────────────────────────────────
    private Token tryMultiLineComment(int startLine, int startCol) {
        if (!matchString("#*")) return null;

        StringBuilder lexeme = new StringBuilder("#*");
        while (position < input.length()) {
            if (peek() == '*' && peekAt(1) == '#') {
                lexeme.append("*#");
                advance(); advance();
                return new Token(TokenType.MULTI_LINE_COMMENT, lexeme.toString(), startLine, startCol);
            }
            lexeme.append(peek());
            advance();
        }
        // ── Error: unclosed comment (Part 3) ────────────────────────────────
        // Recovery: treat entire remainder as comment body, return error token,
        //           scanner reaches EOF and stops — all errors already reported.
        errorHandler.detectUnclosedComment(lexeme.toString(), startLine, startCol);
        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
    }

    private Token trySingleLineComment(int startLine, int startCol) {
        if (!matchString("##")) return null;
        StringBuilder lexeme = new StringBuilder("##");
        while (position < input.length() && peek() != '\n') { lexeme.append(peek()); advance(); }
        return new Token(TokenType.SINGLE_LINE_COMMENT, lexeme.toString(), startLine, startCol);
    }

    // ── Operators ─────────────────────────────────────────────────────────────
    private Token tryMultiCharOperator(int startLine, int startCol) {
        if (position + 1 >= input.length()) return null;
        String two = input.substring(position, position + 2);
        TokenType type = null;
        switch (two) {
            case "**":            type = TokenType.ARITHMETIC_OP; break;
            case "==": case "!=":
            case "<=": case ">=": type = TokenType.RELATIONAL_OP; break;
            case "&&": case "||": type = TokenType.LOGICAL_OP;    break;
            case "+=": case "-=":
            case "*=": case "/=": type = TokenType.ASSIGNMENT_OP; break;
            case "++":            type = TokenType.INCREMENT_OP;  break;
            case "--":            type = TokenType.DECREMENT_OP;  break;
        }
        if (type == null) return null;
        advance(); advance();
        return new Token(type, two, startLine, startCol);
    }

    private Token trySingleCharOperator(int startLine, int startCol) {
        char ch = peek();
        TokenType type = null;
        switch (ch) {
            case '+': case '-': case '*': case '/': case '%': type = TokenType.ARITHMETIC_OP; break;
            case '<': case '>':                               type = TokenType.RELATIONAL_OP; break;
            case '!':                                         type = TokenType.LOGICAL_OP;    break;
            case '=':                                         type = TokenType.ASSIGNMENT_OP; break;
            default: return null;
        }
        advance();
        return new Token(type, String.valueOf(ch), startLine, startCol);
    }

    // ── Keywords & Booleans ───────────────────────────────────────────────────
    private Token tryKeyword(int startLine, int startCol) {
        for (String kw : KEYWORDS) if (matchWord(kw)) return new Token(TokenType.KEYWORD, kw, startLine, startCol);
        return null;
    }
    private Token tryBoolean(int startLine, int startCol) {
        for (String b : BOOLEANS) if (matchWord(b)) return new Token(TokenType.BOOLEAN_LITERAL, b, startLine, startCol);
        return null;
    }
    private boolean matchWord(String word) {
        int saved = position;
        for (int i = 0; i < word.length(); i++) {
            if (position >= input.length() || peek() != word.charAt(i)) { position = saved; return false; }
            advance();
        }
        if (position < input.length()) {
            char next = peek();
            if (Character.isLetterOrDigit(next) || next == '_') { position = saved; return false; }
        }
        return true;
    }

    // ── Identifier ────────────────────────────────────────────────────────────
    private Token tryIdentifier(int startLine, int startCol) {
        if (position >= input.length()) return null;
        char first = peek();

        // ── Error: invalid identifier start (Part 3) ─────────────────────────
        // A word starting with lowercase, digit, or underscore is invalid.
        // Recovery: consume the whole word-like sequence, report once, return ERROR.
        if (Character.isLowerCase(first) || first == '_') {
            StringBuilder bad = new StringBuilder();
            bad.append(first); advance();
            while (position < input.length()) {
                char ch = peek();
                if (Character.isLetterOrDigit(ch) || ch == '_') { bad.append(ch); advance(); }
                else break;
            }
            errorHandler.detectInvalidIdentifier(bad.toString(), startLine, startCol);
            return new Token(TokenType.ERROR, bad.toString(), startLine, startCol);
        }

        if (!Character.isUpperCase(first)) return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append(first); advance();

        while (position < input.length()) {
            char ch = peek();
            if (Character.isLowerCase(ch) || Character.isDigit(ch) || ch == '_') {
                lexeme.append(ch); advance();
            } else break;
        }

        // ── Error: identifier too long (Part 3) ──────────────────────────────
        // Recovery: truncate to 31 chars, report error, continue with truncated name.
        if (lexeme.length() > 31) {
            errorHandler.detectIdentifierTooLong(lexeme.toString(), startLine, startCol);
            lexeme.setLength(31);
        }

        String name = lexeme.toString();
        symbolTable.insert(name, startLine, startCol);
        return new Token(TokenType.IDENTIFIER, name, startLine, startCol);
    }

    // ── Floating-point ────────────────────────────────────────────────────────
    private Token tryFloatingPoint(int startLine, int startCol) {
        int startPos = position;
        StringBuilder lexeme = new StringBuilder();

        if (position < input.length() && (peek() == '+' || peek() == '-')) { lexeme.append(peek()); advance(); }
        if (position >= input.length() || !Character.isDigit(peek())) { position = startPos; return null; }
        while (position < input.length() && Character.isDigit(peek())) { lexeme.append(peek()); advance(); }
        if (position >= input.length() || peek() != '.') { position = startPos; return null; }

        lexeme.append('.'); advance();

        // Count all consecutive fractional digits (may exceed 6)
        StringBuilder fracDigits = new StringBuilder();
        while (position < input.length() && Character.isDigit(peek())) { fracDigits.append(peek()); advance(); }

        if (fracDigits.length() == 0) { position = startPos; return null; }

        // ── Error: too many fractional digits (Part 3) ───────────────────────
        // Recovery: accept the first 6 digits as valid float, leave the rest
        //           to be scanned as a new integer token in the next call.
        if (fracDigits.length() > 6) {
            String validPart = lexeme.toString() + fracDigits.substring(0, 6);
            errorHandler.detectMalformedFloat(
                    lexeme.toString() + fracDigits.toString(), startLine, startCol);
            // Backtrack: put extra digits back
            int extra = fracDigits.length() - 6;
            position -= extra;
            column   -= extra;
            return new Token(TokenType.FLOATING_POINT_LITERAL, validPart, startLine, startCol);
        }

        lexeme.append(fracDigits);

        // Optional exponent
        if (position < input.length() && (peek() == 'e' || peek() == 'E')) {
            int expStart = position;
            lexeme.append(peek()); advance();
            if (position < input.length() && (peek() == '+' || peek() == '-')) { lexeme.append(peek()); advance(); }
            int expDigits = 0;
            while (position < input.length() && Character.isDigit(peek())) { lexeme.append(peek()); advance(); expDigits++; }
            if (expDigits == 0) { position = expStart; lexeme.setLength(expStart - startPos); }
        }

        return new Token(TokenType.FLOATING_POINT_LITERAL, lexeme.toString(), startLine, startCol);
    }

    // ── Integer ───────────────────────────────────────────────────────────────
    private Token tryInteger(int startLine, int startCol) {
        int startPos = position;
        StringBuilder lexeme = new StringBuilder();
        if (position < input.length() && (peek() == '+' || peek() == '-')) { lexeme.append(peek()); advance(); }
        if (position >= input.length() || !Character.isDigit(peek())) { position = startPos; return null; }
        while (position < input.length() && Character.isDigit(peek())) { lexeme.append(peek()); advance(); }
        if (position < input.length() && peek() == '.') { position = startPos; return null; }
        return new Token(TokenType.INTEGER_LITERAL, lexeme.toString(), startLine, startCol);
    }

    // ── String literal ────────────────────────────────────────────────────────
    private Token tryString(int startLine, int startCol) {
        int startPos = position;
        if (peek() != '"') return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"'); advance();

        while (position < input.length()) {
            char ch = peek();
            if (ch == '"')  { lexeme.append('"'); advance(); return new Token(TokenType.STRING_LITERAL, lexeme.toString(), startLine, startCol); }

            // ── Error: unterminated string (Part 3) ──────────────────────────
            // Recovery: stop at newline, report error, return ERROR token.
            //           Scanner continues from NEXT line.
            if (ch == '\n') {
                errorHandler.detectUnterminatedString(lexeme.toString(), startLine, startCol);
                return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
            }
            if (ch == '\\') {
                lexeme.append(ch); advance();
                if (position < input.length()) {
                    char esc = peek();
                    if (esc == '"' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                        lexeme.append(esc); advance();
                    } else {
                        // Invalid escape — report but keep scanning
                        errorHandler.detectUnterminatedString(lexeme.toString(), startLine, startCol);
                        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                    }
                }
            } else { lexeme.append(ch); advance(); }
        }

        // Reached EOF without closing quote
        errorHandler.detectUnterminatedString(lexeme.toString(), startLine, startCol);
        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
    }

    // ── Character literal ─────────────────────────────────────────────────────
    private Token tryCharacter(int startLine, int startCol) {
        int startPos = position;
        if (peek() != '\'') return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append('\''); advance();

        if (position >= input.length()) {
            errorHandler.detectUnterminatedChar(lexeme.toString(), startLine, startCol);
            return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
        }

        char ch = peek();

        // ── Error: empty char literal or newline (Part 3) ────────────────────
        if (ch == '\n' || ch == '\'') {
            errorHandler.detectUnterminatedChar(lexeme.toString(), startLine, startCol);
            if (ch == '\'') advance(); // consume closing quote
            return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
        }

        if (ch == '\\') {
            lexeme.append(ch); advance();
            if (position < input.length()) {
                char esc = peek();
                if (esc == '\'' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                    lexeme.append(esc); advance();
                } else {
                    errorHandler.detectUnterminatedChar(lexeme.toString(), startLine, startCol);
                    return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                }
            }
        } else { lexeme.append(ch); advance(); }

        if (position >= input.length() || peek() != '\'') {
            // ── Error: multi-char literal or missing closing quote (Part 3) ──
            // Recovery: consume until closing quote or whitespace, report, continue.
            while (position < input.length() && peek() != '\'' && peek() != '\n') {
                lexeme.append(peek()); advance();
            }
            if (position < input.length() && peek() == '\'') advance();
            errorHandler.detectUnterminatedChar(lexeme.toString(), startLine, startCol);
            return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
        }

        lexeme.append('\''); advance();
        return new Token(TokenType.CHARACTER_LITERAL, lexeme.toString(), startLine, startCol);
    }

    // ── Punctuators ───────────────────────────────────────────────────────────
    private Token tryPunctuator(int startLine, int startCol) {
        char ch = peek();
        if (ch=='('||ch==')'||ch=='{'||ch=='}'||ch=='['||ch==']'||ch==','||ch==';'||ch==':') {
            advance(); return new Token(TokenType.PUNCTUATOR, String.valueOf(ch), startLine, startCol);
        }
        return null;
    }

    // ── Whitespace ────────────────────────────────────────────────────────────
    private Token tryWhitespace(int startLine, int startCol) {
        if (position >= input.length()) return null;
        char ch = peek();
        if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') return null;
        StringBuilder lexeme = new StringBuilder();
        while (position < input.length()) {
            ch = peek();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') { lexeme.append(ch); whitespaceRemoved++; advance(); }
            else break;
        }
        return new Token(TokenType.WHITESPACE, lexeme.toString(), startLine, startCol);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private char peek()              { return position >= input.length() ? '\0' : input.charAt(position); }
    private char peekAt(int offset)  { int p = position+offset; return p >= input.length() ? '\0' : input.charAt(p); }

    private void advance() {
        if (position < input.length()) {
            if (input.charAt(position) == '\n') { line++; column = 1; } else column++;
            position++;
        }
    }

    private boolean matchString(String str) {
        if (position + str.length() > input.length()) return false;
        for (int i = 0; i < str.length(); i++) if (input.charAt(position+i) != str.charAt(i)) return false;
        for (int i = 0; i < str.length(); i++) advance();
        return true;
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);

        File testsDir = new File("." + File.separator + "tests");
        if (!testsDir.exists() || !testsDir.isDirectory()) {
            System.out.println("ERROR: tests folder not found at: " + testsDir.getAbsolutePath());
            userInput.close(); return;
        }

        File[] kwFiles = testsDir.listFiles(f -> f.getName().endsWith(".kw"));
        if (kwFiles == null || kwFiles.length == 0) {
            System.out.println("ERROR: No .kw files found."); userInput.close(); return;
        }
        Arrays.sort(kwFiles, (a, b) -> a.getName().compareTo(b.getName()));

        System.out.println("=========================");
        System.out.println("|     MANUAL SCANNER     |");
        System.out.println("=========================");
        System.out.println("Available test files:");
        System.out.println("-------------------------------------------");
        for (int i = 0; i < kwFiles.length; i++) System.out.printf("  [%d] %s%n", i+1, kwFiles[i].getName());
        System.out.println("-------------------------------------------");
        System.out.print("Enter file number (1-" + kwFiles.length + "): ");

        int choice = -1;
        while (choice < 1 || choice > kwFiles.length) {
            try {
                choice = Integer.parseInt(userInput.nextLine().trim());
                if (choice < 1 || choice > kwFiles.length)
                    System.out.print("Invalid. Enter (1-" + kwFiles.length + "): ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid. Enter (1-" + kwFiles.length + "): ");
            }
        }

        File selectedFile = kwFiles[choice - 1];
        System.out.println("\nReading: " + selectedFile.getName());
        System.out.println("===========================================");

        try {
            String input = new String(java.nio.file.Files.readAllBytes(selectedFile.toPath()));
            ManualScanner scanner = new ManualScanner(input);
            List<Token>   tokens  = scanner.scanAll();

            // Part A
            System.out.println("\n=== TOKENS ===");
            int tokenCount = 0;
            for (Token token : tokens) {
                if (token.getType() != TokenType.WHITESPACE) {
                    System.out.println(token);
                    tokenCount++;
                }
            }

            // Part B
            System.out.println("\n=== PRE-PROCESSING (Part B) ===");
            System.out.println("File               : " + selectedFile.getName());
            System.out.println("Total tokens       : " + tokenCount);
            System.out.println("Whitespace removed : " + scanner.getWhitespaceRemoved());
            System.out.println("Lines processed    : " + scanner.getCurrentLine());

            // Part E
            scanner.getSymbolTable().display();

            // Part 3
            scanner.getErrorHandler().displayErrors();
            scanner.getErrorHandler().displaySummary();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        userInput.close();
    }
}