import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String source;
    private int pos = 0;
    private int line = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            char c = peek();

            // Skip whitespace and newlines
            if (c == '\n') {
                line++;
                pos++;
                continue;
            }
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // Identifiers and keywords
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
                continue;
            }

            // Numbers (integers and decimals)
            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }

            // Assignment operator ":="
            if (c == ':') {
                if (peekNext() == '=') {
                    int startLine = line;
                    pos += 2;
                    tokens.add(new Token(TokenType.ASSIGN, ":=", startLine));
                } else {
                    // ':' alone is not valid in this language
                    tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), line));
                    pos++;
                }
                continue;
            }

            switch (c) {
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+", line));
                    pos++;
                    continue;
                case '-':
                    tokens.add(new Token(TokenType.MINUS, "-", line));
                    pos++;
                    continue;
                case '*':
                    tokens.add(new Token(TokenType.STAR, "*", line));
                    pos++;
                    continue;
                case '/':
                    tokens.add(new Token(TokenType.SLASH, "/", line));
                    pos++;
                    continue;
                case '(':
                    tokens.add(new Token(TokenType.LEFT_PAREN, "(", line));
                    pos++;
                    continue;
                case ')':
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")", line));
                    pos++;
                    continue;
                case ';':
                    tokens.add(new Token(TokenType.SEMICOLON, ";", line));
                    pos++;
                    continue;
                default:
                    // Unexpected character: record it but keep scanning (no crash)
                    tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), line));
                    pos++;
                    continue;
            }
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private Token readIdentifierOrKeyword() {
        int start = pos;
        int startLine = line;
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            pos++;
        }
        String text = source.substring(start, pos);

        switch (text) {
            case "num":
                return new Token(TokenType.NUM, text, startLine);
            case "print":
                return new Token(TokenType.PRINT, text, startLine);
            default:
                return new Token(TokenType.IDENTIFIER, text, startLine);
        }
    }

    private Token readNumber() {
        int start = pos;
        int startLine = line;
        while (!isAtEnd() && Character.isDigit(peek())) {
            pos++;
        }
        // Optional decimal part
        if (!isAtEnd() && peek() == '.' && Character.isDigit(peekNext())) {
            pos++; // consume '.'
            while (!isAtEnd() && Character.isDigit(peek())) {
                pos++;
            }
        }
        String text = source.substring(start, pos);
        return new Token(TokenType.NUMBER, text, startLine);
    }

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private char peek() {
        return source.charAt(pos);
    }

    private char peekNext() {
        if (pos + 1 >= source.length()) return '\0';
        return source.charAt(pos + 1);
    }
}
