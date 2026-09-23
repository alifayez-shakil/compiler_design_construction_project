package compiler;
public enum TokenType {
    // Keywords
    GONA,       // integer type
    DOSHOMIK,   // decimal type
    JODI,       // if
    NOHOILE,    // else
    JOTOKHON,   // while
    KOIYO,      // print

    // Literals / identifiers
    IDENTIFIER,
    NUMBER,

    // Assignment
    ASSIGN,     // :=

    // Arithmetic operators
    PLUS, MINUS, STAR, SLASH, PERCENT,

    // Comparison / equality operators
    GT, GE, LT, LE, EQ, NEQ,

    // Symbols
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    SEMICOLON,

    UNKNOWN,
    EOF
}
