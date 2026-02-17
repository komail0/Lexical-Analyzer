import java.io.*;
import java.util.*;

public class ManualScanner {
    private String input;
    private int position;
    private int line;
    private int column;

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "finish", "loop", "condition", "declare", "output",
            "input", "function", "return", "break", "continue", "else"
    ));

    private static final Set<String> BOOLEANS = new HashSet<>(Arrays.asList("true", "false"));

    public ManualScanner(String input) {
        this.input = input;
        this.position = 0;
        this.line = 1;
        this.column = 1;
    }

    public Token getNextToken() {
        if (position >= input.length()) return null;

        int startLine = line;
        int startCol = column;
        Token token;

        // Pattern matching priority (Section 3.12)
        if ((token = tryMultiLineComment(startLine, startCol)) != null) return token;
        if ((token = trySingleLineComment(startLine, startCol)) != null) return token;
        if ((token = tryMultiCharOperator(startLine, startCol)) != null) return token;
        if ((token = tryKeyword(startLine, startCol)) != null) return token;
        if ((token = tryBoolean(startLine, startCol)) != null) return token;
        if ((token = tryIdentifier(startLine, startCol)) != null) return token;
        if ((token = tryFloatingPoint(startLine, startCol)) != null) return token;
        if ((token = tryInteger(startLine, startCol)) != null) return token;
        if ((token = tryString(startLine, startCol)) != null) return token;
        if ((token = tryCharacter(startLine, startCol)) != null) return token;
        if ((token = trySingleCharOperator(startLine, startCol)) != null) return token;
        if ((token = tryPunctuator(startLine, startCol)) != null) return token;
        if ((token = tryWhitespace(startLine, startCol)) != null) return token;

        char ch = peek();
        advance();
        return new Token(TokenType.ERROR, String.valueOf(ch), startLine, startCol);
    }

    private Token tryMultiLineComment(int startLine, int startCol) {
        int startPos = position;
        if (!matchString("#*")) return null;

        StringBuilder lexeme = new StringBuilder("#*");
        while (position < input.length()) {
            if (peek() == '*' && peekAhead(1) == '#') {
                lexeme.append("*#");
                advance();
                advance();
                return new Token(TokenType.MULTI_LINE_COMMENT, lexeme.toString(), startLine, startCol);
            }
            lexeme.append(peek());
            advance();
        }
        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
    }

    private Token trySingleLineComment(int startLine, int startCol) {
        int startPos = position;
        if (!matchString("##")) return null;

        StringBuilder lexeme = new StringBuilder("##");
        while (position < input.length() && peek() != '\n') {
            lexeme.append(peek());
            advance();
        }
        return new Token(TokenType.SINGLE_LINE_COMMENT, lexeme.toString(), startLine, startCol);
    }

    private Token tryMultiCharOperator(int startLine, int startCol) {
        if (position + 1 >= input.length()) return null;

        String twoChar = input.substring(position, position + 2);
        TokenType type = null;

        switch (twoChar) {
            case "**": type = TokenType.ARITHMETIC_OP; break;
            case "==": case "!=": case "<=": case ">=": type = TokenType.RELATIONAL_OP; break;
            case "&&": case "||": type = TokenType.LOGICAL_OP; break;
            case "+=": case "-=": case "*=": case "/=": type = TokenType.ASSIGNMENT_OP; break;
            case "++": type = TokenType.INCREMENT_OP; break;
            case "--": type = TokenType.DECREMENT_OP; break;
        }

        if (type != null) {
            advance();
            advance();
            return new Token(type, twoChar, startLine, startCol);
        }
        return null;
    }

    private Token trySingleCharOperator(int startLine, int startCol) {
        char ch = peek();
        TokenType type = null;

        switch (ch) {
            case '+': case '-': case '*': case '/': case '%': type = TokenType.ARITHMETIC_OP; break;
            case '<': case '>': type = TokenType.RELATIONAL_OP; break;
            case '!': type = TokenType.LOGICAL_OP; break;
            case '=': type = TokenType.ASSIGNMENT_OP; break;
            default: return null;
        }

        advance();
        return new Token(type, String.valueOf(ch), startLine, startCol);
    }

    private Token tryKeyword(int startLine, int startCol) {
        for (String keyword : KEYWORDS) {
            if (matchWord(keyword)) {
                return new Token(TokenType.KEYWORD, keyword, startLine, startCol);
            }
        }
        return null;
    }

    private Token tryBoolean(int startLine, int startCol) {
        for (String bool : BOOLEANS) {
            if (matchWord(bool)) {
                return new Token(TokenType.BOOLEAN_LITERAL, bool, startLine, startCol);
            }
        }
        return null;
    }

    private boolean matchWord(String word) {
        int startPos = position;
        for (int i = 0; i < word.length(); i++) {
            if (position >= input.length() || peek() != word.charAt(i)) {
                position = startPos;
                return false;
            }
            advance();
        }
        if (position < input.length()) {
            char next = peek();
            if (Character.isLetterOrDigit(next) || next == '_') {
                position = startPos;
                return false;
            }
        }
        return true;
    }

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
            } else {
                break;
            }
        }
        return new Token(TokenType.IDENTIFIER, lexeme.toString(), startLine, startCol);
    }

    private Token tryFloatingPoint(int startLine, int startCol) {
        int startPos = position;
        StringBuilder lexeme = new StringBuilder();

        if (position < input.length() && (peek() == '+' || peek() == '-')) {
            lexeme.append(peek());
            advance();
        }

        if (position >= input.length() || !Character.isDigit(peek())) {
            position = startPos;
            return null;
        }

        while (position < input.length() && Character.isDigit(peek())) {
            lexeme.append(peek());
            advance();
        }

        if (position >= input.length() || peek() != '.') {
            position = startPos;
            return null;
        }
        lexeme.append('.');
        advance();

        int fracDigits = 0;
        while (position < input.length() && Character.isDigit(peek()) && fracDigits < 6) {
            lexeme.append(peek());
            advance();
            fracDigits++;
        }

        if (fracDigits == 0 || fracDigits > 6) {
            position = startPos;
            return null;
        }

        if (position < input.length() && (peek() == 'e' || peek() == 'E')) {
            int expStart = position;
            lexeme.append(peek());
            advance();

            if (position < input.length() && (peek() == '+' || peek() == '-')) {
                lexeme.append(peek());
                advance();
            }

            int expDigits = 0;
            while (position < input.length() && Character.isDigit(peek())) {
                lexeme.append(peek());
                advance();
                expDigits++;
            }

            if (expDigits == 0) {
                position = expStart;
                lexeme.setLength(lexeme.length() - (position - expStart));
            }
        }

        return new Token(TokenType.FLOATING_POINT_LITERAL, lexeme.toString(), startLine, startCol);
    }

    private Token tryInteger(int startLine, int startCol) {
        int startPos = position;
        StringBuilder lexeme = new StringBuilder();

        if (position < input.length() && (peek() == '+' || peek() == '-')) {
            lexeme.append(peek());
            advance();
        }

        if (position >= input.length() || !Character.isDigit(peek())) {
            position = startPos;
            return null;
        }

        while (position < input.length() && Character.isDigit(peek())) {
            lexeme.append(peek());
            advance();
        }

        if (position < input.length() && peek() == '.') {
            position = startPos;
            return null;
        }

        return new Token(TokenType.INTEGER_LITERAL, lexeme.toString(), startLine, startCol);
    }

    private Token tryString(int startLine, int startCol) {
        int startPos = position;
        if (peek() != '"') return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"');
        advance();

        while (position < input.length()) {
            char ch = peek();
            if (ch == '"') {
                lexeme.append('"');
                advance();
                return new Token(TokenType.STRING_LITERAL, lexeme.toString(), startLine, startCol);
            }
            if (ch == '\n') {
                position = startPos;
                return null;
            }
            if (ch == '\\') {
                lexeme.append(ch);
                advance();
                if (position < input.length()) {
                    char escaped = peek();
                    if (escaped == '"' || escaped == '\\' || escaped == 'n' ||
                            escaped == 't' || escaped == 'r') {
                        lexeme.append(escaped);
                        advance();
                    } else {
                        position = startPos;
                        return null;
                    }
                }
            } else {
                lexeme.append(ch);
                advance();
            }
        }
        position = startPos;
        return null;
    }

    private Token tryCharacter(int startLine, int startCol) {
        int startPos = position;
        if (peek() != '\'') return null;

        StringBuilder lexeme = new StringBuilder();
        lexeme.append('\'');
        advance();

        if (position >= input.length()) {
            position = startPos;
            return null;
        }

        char ch = peek();
        if (ch == '\n' || ch == '\'') {
            position = startPos;
            return null;
        }

        if (ch == '\\') {
            lexeme.append(ch);
            advance();
            if (position < input.length()) {
                char escaped = peek();
                if (escaped == '\'' || escaped == '\\' || escaped == 'n' ||
                        escaped == 't' || escaped == 'r') {
                    lexeme.append(escaped);
                    advance();
                } else {
                    position = startPos;
                    return null;
                }
            }
        } else {
            lexeme.append(ch);
            advance();
        }

        if (position >= input.length() || peek() != '\'') {
            position = startPos;
            return null;
        }

        lexeme.append('\'');
        advance();
        return new Token(TokenType.CHARACTER_LITERAL, lexeme.toString(), startLine, startCol);
    }

    private Token tryPunctuator(int startLine, int startCol) {
        char ch = peek();
        if (ch == '(' || ch == ')' || ch == '{' || ch == '}' ||
                ch == '[' || ch == ']' || ch == ',' || ch == ';' || ch == ':') {
            advance();
            return new Token(TokenType.PUNCTUATOR, String.valueOf(ch), startLine, startCol);
        }
        return null;
    }

    private Token tryWhitespace(int startLine, int startCol) {
        if (position >= input.length()) return null;

        char ch = peek();
        if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') return null;

        StringBuilder lexeme = new StringBuilder();
        while (position < input.length()) {
            ch = peek();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                lexeme.append(ch);
                advance();
            } else {
                break;
            }
        }
        return new Token(TokenType.WHITESPACE, lexeme.toString(), startLine, startCol);
    }

    private char peek() {
        return (position >= input.length()) ? '\0' : input.charAt(position);
    }

    private char peekAhead(int offset) {
        int pos = position + offset;
        return (pos >= input.length()) ? '\0' : input.charAt(pos);
    }

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

        for (int i = 0; i < str.length(); i++) {
            if (input.charAt(position + i) != str.charAt(i)) return false;
        }

        for (int i = 0; i < str.length(); i++) {
            advance();
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ManualScanner <input_file>");
            return;
        }

        try {
            String input = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(args[0])));

            ManualScanner scanner = new ManualScanner(input);
            Token token;

            while ((token = scanner.getNextToken()) != null) {
                if (token.getType() != TokenType.WHITESPACE) {
                    System.out.println(token);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}