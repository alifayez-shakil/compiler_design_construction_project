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

            if (c == '\n') {
                line++;
                pos++;
                continue;
            }
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // Line comment: # ... end of line
            if (c == '#') {
                while (!isAtEnd() && peek() != '\n') pos++;
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
                continue;
            }

            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }

            // ':=' assignment
            if (c == ':') {
                if (peekNext() == '=') {
                    int startLine = line;
                    pos += 2;
                    tokens.add(new Token(TokenType.ASSIGN, ":=", startLine));
                } else {
                    tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), line));
                    pos++;
                }
                continue;
            }

            // '==' equality (bare '=' is not valid in this language)
            if (c == '=') {
                if (peekNext() == '=') {
                    int startLine = line;
                    pos += 2;
                    tokens.add(new Token(TokenType.EQ, "==", startLine));
                } else {
                    tokens.add(new Token(TokenType.UNKNOWN, "=", line));
                    pos++;
                }
                continue;
            }

            // '!=' not-equal (bare '!' is not valid)
            if (c == '!') {
                if (peekNext() == '=') {
                    int startLine = line;
                    pos += 2;
                    tokens.add(new Token(TokenType.NEQ, "!=", startLine));
                } else {
                    tokens.add(new Token(TokenType.UNKNOWN, "!", line));
                    pos++;
                }
                continue;
            }

            // '>' or '>='
            if (c == '>') {
                if (peekNext() == '=') {
                    int startLine = line;
                    pos += 2;
                    tokens.add(new Token(TokenType.GE, ">=", startLine));
                } else {
                    tokens.add(new Token(TokenType.GT, ">", line));
                    pos++;
                }
                continue;
            }

            // '<' or '<='
            if (c == '<') {
                if (peekNext() == '=') {
                    int startLine = line;
                    pos += 2;
                    tokens.add(new Token(TokenType.LE, "<=", startLine));
                } else {
                    tokens.add(new Token(TokenType.LT, "<", line));
                    pos++;
                }
                continue;
            }

            switch (c) {
                case '+': tokens.add(new Token(TokenType.PLUS, "+", line)); pos++; continue;
                case '-': tokens.add(new Token(TokenType.MINUS, "-", line)); pos++; continue;
                case '*': tokens.add(new Token(TokenType.STAR, "*", line)); pos++; continue;
                case '/': tokens.add(new Token(TokenType.SLASH, "/", line)); pos++; continue;
                case '%': tokens.add(new Token(TokenType.PERCENT, "%", line)); pos++; continue;
                case '(': tokens.add(new Token(TokenType.LEFT_PAREN, "(", line)); pos++; continue;
                case ')': tokens.add(new Token(TokenType.RIGHT_PAREN, ")", line)); pos++; continue;
                case '{': tokens.add(new Token(TokenType.LEFT_BRACE, "{", line)); pos++; continue;
                case '}': tokens.add(new Token(TokenType.RIGHT_BRACE, "}", line)); pos++; continue;
                case ';': tokens.add(new Token(TokenType.SEMICOLON, ";", line)); pos++; continue;
                default:
                    // Unexpected character: record it but keep scanning, never crash
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
            case "gona":     return new Token(TokenType.GONA, text, startLine);
            case "doshomik": return new Token(TokenType.DOSHOMIK, text, startLine);
            case "jodi":     return new Token(TokenType.JODI, text, startLine);
            case "nohoile":  return new Token(TokenType.NOHOILE, text, startLine);
            case "jotokhon": return new Token(TokenType.JOTOKHON, text, startLine);
            case "koiyo":    return new Token(TokenType.KOIYO, text, startLine);
            default:         return new Token(TokenType.IDENTIFIER, text, startLine);
        }
    }

    private Token readNumber() {
        int start = pos;
        int startLine = line;
        while (!isAtEnd() && Character.isDigit(peek())) {
            pos++;
        }
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
