import java.io.*;
import java.util.*;

/**
 * ManualScanner - Lexical Analyzer
 * Part A: Token Recognition  (25 marks)
 * Part B: Pre-processing     ( 5 marks)
 * Part E: Symbol Table       ( 5 marks)
 */
public class ManualScanner {

    private String      input;
    private int         position;
    private int         line;
    private int         column;
    private int         whitespaceRemoved; // Part B
    private SymbolTable symbolTable;       // Part E

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "finish", "loop", "condition", "declare", "output",
            "input", "function", "return", "break", "continue", "else"
    ));

    private static final Set<String> BOOLEANS = new HashSet<>(Arrays.asList(
            "true", "false"
    ));

    public ManualScanner(String input) {
        this.input             = input;
        this.position          = 0;
        this.line              = 1;
        this.column            = 1;
        this.whitespaceRemoved = 0;
        this.symbolTable       = new SymbolTable();
    }

    // Getters for Part B stats and Part E symbol table
    public int         getWhitespaceRemoved() { return whitespaceRemoved; }
    public int         getCurrentLine()        { return line;             }
    public SymbolTable getSymbolTable()        { return symbolTable;      }

    // Scan entire input and return all tokens
    public List<Token> scanAll() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = getNextToken()) != null) {
            tokens.add(token);
        }
        return tokens;
    }

    // ----------------------------------------------------------------
    // Core: get next token  (pattern priority per Section 3.12)
    // ----------------------------------------------------------------
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

        char ch = peek();
        advance();
        return new Token(TokenType.ERROR, String.valueOf(ch), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Comments
    // ----------------------------------------------------------------
    private Token tryMultiLineComment(int startLine, int startCol) {
        int startPos = position;
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
        // Unclosed comment
        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
    }

    private Token trySingleLineComment(int startLine, int startCol) {
        if (!matchString("##")) return null;

        StringBuilder lexeme = new StringBuilder("##");
        while (position < input.length() && peek() != '\n') {
            lexeme.append(peek());
            advance();
        }
        return new Token(TokenType.SINGLE_LINE_COMMENT, lexeme.toString(), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Operators
    // ----------------------------------------------------------------
    private Token tryMultiCharOperator(int startLine, int startCol) {
        if (position + 1 >= input.length()) return null;

        String    two  = input.substring(position, position + 2);
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
        char      ch   = peek();
        TokenType type = null;

        switch (ch) {
            case '+': case '-': case '*': case '/': case '%':
                type = TokenType.ARITHMETIC_OP; break;
            case '<': case '>':
                type = TokenType.RELATIONAL_OP; break;
            case '!':
                type = TokenType.LOGICAL_OP;    break;
            case '=':
                type = TokenType.ASSIGNMENT_OP; break;
            default: return null;
        }
        advance();
        return new Token(type, String.valueOf(ch), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Keywords and Booleans  (must come before identifiers)
    // ----------------------------------------------------------------
    private Token tryKeyword(int startLine, int startCol) {
        for (String kw : KEYWORDS)
            if (matchWord(kw))
                return new Token(TokenType.KEYWORD, kw, startLine, startCol);
        return null;
    }

    private Token tryBoolean(int startLine, int startCol) {
        for (String b : BOOLEANS)
            if (matchWord(b))
                return new Token(TokenType.BOOLEAN_LITERAL, b, startLine, startCol);
        return null;
    }

    // Match a complete word (not a prefix of a longer identifier)
    private boolean matchWord(String word) {
        int saved = position;
        for (int i = 0; i < word.length(); i++) {
            if (position >= input.length() || peek() != word.charAt(i)) {
                position = saved;
                return false;
            }
            advance();
        }
        // Ensure not followed by identifier character
        if (position < input.length()) {
            char next = peek();
            if (Character.isLetterOrDigit(next) || next == '_') {
                position = saved;
                return false;
            }
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Identifier: [A-Z][a-z0-9_]{0,30}
    // Part E: every identifier is inserted into the symbol table
    // ----------------------------------------------------------------
    private Token tryIdentifier(int startLine, int startCol) {
        if (position >= input.length() || !Character.isUpperCase(peek())) return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append(peek());
        advance();

        while (position < input.length() && lexeme.length() < 31) {
            char ch = peek();
            if (Character.isLowerCase(ch) || Character.isDigit(ch) || ch == '_') {
                lexeme.append(ch);
                advance();
            } else break;
        }

        String name = lexeme.toString();
        symbolTable.insert(name, startLine, startCol);   // Part E
        return new Token(TokenType.IDENTIFIER, name, startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Floating-point: [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    // Must be tried BEFORE integer (longest match)
    // ----------------------------------------------------------------
    private Token tryFloatingPoint(int startLine, int startCol) {
        int           startPos = position;
        StringBuilder lexeme   = new StringBuilder();

        // Optional sign
        if (position < input.length() && (peek() == '+' || peek() == '-')) {
            lexeme.append(peek()); advance();
        }

        // Integer part (required)
        if (position >= input.length() || !Character.isDigit(peek())) {
            position = startPos; return null;
        }
        while (position < input.length() && Character.isDigit(peek())) {
            lexeme.append(peek()); advance();
        }

        // Decimal point (required for float)
        if (position >= input.length() || peek() != '.') {
            position = startPos; return null;
        }
        lexeme.append('.'); advance();

        // Fractional digits (1-6 required)
        int frac = 0;
        while (position < input.length() && Character.isDigit(peek()) && frac < 6) {
            lexeme.append(peek()); advance(); frac++;
        }
        if (frac == 0 || frac > 6) { position = startPos; return null; }

        // Optional exponent
        if (position < input.length() && (peek() == 'e' || peek() == 'E')) {
            int expStart = position;
            lexeme.append(peek()); advance();
            if (position < input.length() && (peek() == '+' || peek() == '-')) {
                lexeme.append(peek()); advance();
            }
            int expDigits = 0;
            while (position < input.length() && Character.isDigit(peek())) {
                lexeme.append(peek()); advance(); expDigits++;
            }
            if (expDigits == 0) {
                position = expStart;
                lexeme.setLength(expStart - startPos);
            }
        }

        return new Token(TokenType.FLOATING_POINT_LITERAL, lexeme.toString(), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Integer: [+-]?[0-9]+
    // ----------------------------------------------------------------
    private Token tryInteger(int startLine, int startCol) {
        int           startPos = position;
        StringBuilder lexeme   = new StringBuilder();

        // Optional sign
        if (position < input.length() && (peek() == '+' || peek() == '-')) {
            lexeme.append(peek()); advance();
        }

        // Digits (required)
        if (position >= input.length() || !Character.isDigit(peek())) {
            position = startPos; return null;
        }
        while (position < input.length() && Character.isDigit(peek())) {
            lexeme.append(peek()); advance();
        }

        // If followed by '.', this is actually a float
        if (position < input.length() && peek() == '.') {
            position = startPos; return null;
        }

        return new Token(TokenType.INTEGER_LITERAL, lexeme.toString(), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // String literal: "([ ^"\\\n]|\\["\\ntr])*"
    // Part B: whitespace INSIDE strings is preserved
    // ----------------------------------------------------------------
    private Token tryString(int startLine, int startCol) {
        int startPos = position;
        if (peek() != '"') return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"'); advance();

        while (position < input.length()) {
            char ch = peek();
            if (ch == '"') {
                lexeme.append('"'); advance();
                return new Token(TokenType.STRING_LITERAL, lexeme.toString(), startLine, startCol);
            }
            if (ch == '\n') { position = startPos; return null; }
            if (ch == '\\') {
                lexeme.append(ch); advance();
                if (position < input.length()) {
                    char esc = peek();
                    if (esc == '"' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                        lexeme.append(esc); advance();
                    } else { position = startPos; return null; }
                }
            } else {
                // Part B: all chars including spaces preserved inside string
                lexeme.append(ch); advance();
            }
        }
        position = startPos; return null;
    }

    // ----------------------------------------------------------------
    // Character literal: '([ ^'\\\n]|\\['\\ntr])'
    // ----------------------------------------------------------------
    private Token tryCharacter(int startLine, int startCol) {
        int startPos = position;
        if (peek() != '\'') return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append('\''); advance();

        if (position >= input.length()) { position = startPos; return null; }

        char ch = peek();
        if (ch == '\n' || ch == '\'') { position = startPos; return null; }

        if (ch == '\\') {
            lexeme.append(ch); advance();
            if (position < input.length()) {
                char esc = peek();
                if (esc == '\'' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                    lexeme.append(esc); advance();
                } else { position = startPos; return null; }
            }
        } else {
            lexeme.append(ch); advance();
        }

        if (position >= input.length() || peek() != '\'') { position = startPos; return null; }
        lexeme.append('\''); advance();
        return new Token(TokenType.CHARACTER_LITERAL, lexeme.toString(), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Punctuators: ( ) { } [ ] , ; :
    // ----------------------------------------------------------------
    private Token tryPunctuator(int startLine, int startCol) {
        char ch = peek();
        if (ch == '(' || ch == ')' || ch == '{' || ch == '}' ||
                ch == '[' || ch == ']' || ch == ',' || ch == ';' || ch == ':') {
            advance();
            return new Token(TokenType.PUNCTUATOR, String.valueOf(ch), startLine, startCol);
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Whitespace: [ \t\r\n]+
    // Part B: removed from output, count tracked, line/col updated
    // ----------------------------------------------------------------
    private Token tryWhitespace(int startLine, int startCol) {
        if (position >= input.length()) return null;
        char ch = peek();
        if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') return null;

        StringBuilder lexeme = new StringBuilder();
        while (position < input.length()) {
            ch = peek();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                lexeme.append(ch);
                whitespaceRemoved++;   // Part B: count removed whitespace
                advance();             // Part B: advance updates line/column
            } else break;
        }
        return new Token(TokenType.WHITESPACE, lexeme.toString(), startLine, startCol);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private char peek() {
        return position >= input.length() ? '\0' : input.charAt(position);
    }

    private char peekAt(int offset) {
        int pos = position + offset;
        return pos >= input.length() ? '\0' : input.charAt(pos);
    }

    // Part B: advance tracks line and column numbers accurately
    private void advance() {
        if (position < input.length()) {
            if (input.charAt(position) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            position++;
        }
    }

    private boolean matchString(String str) {
        if (position + str.length() > input.length()) return false;
        for (int i = 0; i < str.length(); i++)
            if (input.charAt(position + i) != str.charAt(i)) return false;
        for (int i = 0; i < str.length(); i++) advance();
        return true;
    }

    // ----------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);

        // Go one directory up, then into the tests folder
        String testsPath = "." + File.separator + "tests";
        File   testsDir  = new File(testsPath);

        // Check the tests folder exists
        if (!testsDir.exists() || !testsDir.isDirectory()) {
            System.out.println("ERROR: tests folder not found at: " + testsDir.getAbsolutePath());
            userInput.close();
            return;
        }

        // Find all .kw files in the tests folder
        File[] kwFiles = testsDir.listFiles(f -> f.getName().endsWith(".kw"));

        if (kwFiles == null || kwFiles.length == 0) {
            System.out.println("ERROR: No .kw files found in: " + testsDir.getAbsolutePath());
            userInput.close();
            return;
        }

        // Sort files by name so they appear in order (test1.kw, test2.kw ...)
        Arrays.sort(kwFiles, (a, b) -> a.getName().compareTo(b.getName()));

        // Show menu to user
        System.out.println("===========================================");
        System.out.println("         MANUAL SCANNER                    ");
        System.out.println("===========================================");
        System.out.println("Available test files:");
        System.out.println("-------------------------------------------");
        for (int i = 0; i < kwFiles.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, kwFiles[i].getName());
        }
        System.out.println("-------------------------------------------");
        System.out.print("Enter file number (1-" + kwFiles.length + "): ");

        // Read and validate user choice
        int choice = -1;
        while (choice < 1 || choice > kwFiles.length) {
            try {
                choice = Integer.parseInt(userInput.nextLine().trim());
                if (choice < 1 || choice > kwFiles.length) {
                    System.out.print("Invalid choice. Enter a number (1-" + kwFiles.length + "): ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number (1-" + kwFiles.length + "): ");
            }
        }

        // Read selected file
        File selectedFile = kwFiles[choice - 1];
        System.out.println("\nReading: " + selectedFile.getName());
        System.out.println("===========================================");

        try {
            String input = new String(java.nio.file.Files.readAllBytes(selectedFile.toPath()));

            ManualScanner scanner = new ManualScanner(input);
            List<Token>   tokens  = scanner.scanAll();

            // Part A: Print all tokens
            System.out.println("\n=== TOKENS ===");
            int tokenCount = 0;
            for (Token token : tokens) {
                if (token.getType() != TokenType.WHITESPACE) {
                    System.out.println(token);
                    tokenCount++;
                }
            }

            // Part B: Pre-processing summary
            System.out.println("\n=== PRE-PROCESSING (Part B) ===");
            System.out.println("File               : " + selectedFile.getName());
            System.out.println("Total tokens       : " + tokenCount);
            System.out.println("Whitespace removed : " + scanner.getWhitespaceRemoved());
            System.out.println("Lines processed    : " + scanner.getCurrentLine());

            // Part E: Symbol table
            scanner.getSymbolTable().display();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        userInput.close();
    }
}