public class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    @Override
    public String toString() {
        // Pad the type name so tokens print in neat columns
        return String.format("%-12s %s", type, lexeme);
    }
}
