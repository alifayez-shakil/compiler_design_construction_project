public enum TokenType {
    // Keywords
    NUM,
    PRINT,

    // Literals / identifiers
    IDENTIFIER,
    NUMBER,

    // Operators
    ASSIGN,   // :=
    PLUS,     // +
    MINUS,    // -
    STAR,     // *
    SLASH,    // /

    // Symbols
    LEFT_PAREN,   // (
    RIGHT_PAREN,  // )
    SEMICOLON,    // ;

    // Special
    UNKNOWN,
    EOF
}
