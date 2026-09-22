import java.util.List;

/**
 * A simple Recursive Descent Parser.
 *
 * Grammar:
 *   program         -> statement* EOF
 *   statement       -> declaration | printStatement
 *   declaration     -> "num" IDENTIFIER ":=" expression ";"
 *   printStatement  -> "print" "(" expression ")" ";"
 *   expression      -> term (("+" | "-") term)*
 *   term            -> factor (("*" | "/") factor)*
 *   factor          -> NUMBER | IDENTIFIER | "(" expression ")"
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private boolean hadError = false;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Returns true if the whole program parsed without syntax errors. */
    public boolean parseProgram() {
        while (!isAtEnd()) {
            hadError = false; // reset per-statement flag
            parseStatement();
            // If this statement failed, synchronize to the next likely
            // statement boundary so we can keep checking the rest of
            // the file instead of stopping at the first error.
            if (hadError) {
                overallFailed = true;
                synchronize();
            }
        }
        return !overallFailed;
    }

    private void parseStatement() {
        if (check(TokenType.NUM)) {
            parseDeclaration();
        } else if (check(TokenType.PRINT)) {
            parsePrintStatement();
        } else {
            error("Expected a statement (declaration or print) but found '" + peek().lexeme + "'.");
            advance(); // consume the unexpected token so we make progress
        }
    }

    private void parseDeclaration() {
        advance(); // consume 'num'

        if (!check(TokenType.IDENTIFIER)) {
            error("Expected identifier after 'num'.");
            return;
        }
        advance(); // consume identifier

        if (!match(TokenType.ASSIGN)) {
            error("Expected ':=' after identifier in declaration.");
            return;
        }

        if (!parseExpression()) {
            error("Expected expression after ':=' in declaration.");
            return;
        }

        if (!match(TokenType.SEMICOLON)) {
            error("Expected ';' after declaration.");
        }
    }

    private void parsePrintStatement() {
        advance(); // consume 'print'

        if (!match(TokenType.LEFT_PAREN)) {
            error("Expected '(' after 'print'.");
            return;
        }

        if (!parseExpression()) {
            error("Expected expression inside print(...).");
            return;
        }

        if (!match(TokenType.RIGHT_PAREN)) {
            error("Expected ')' after expression in print statement.");
            return;
        }

        if (!match(TokenType.SEMICOLON)) {
            error("Expected ';' after print statement.");
        }
    }

    /** expression -> term (("+" | "-") term)* */
    private boolean parseExpression() {
        if (!parseTerm()) return false;

        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            advance(); // consume operator
            if (!parseTerm()) {
                error("Expected expression after '+' or '-'.");
                return false;
            }
        }
        return true;
    }

    /** term -> factor (("*" | "/") factor)* */
    private boolean parseTerm() {
        if (!parseFactor()) return false;

        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            advance(); // consume operator
            if (!parseFactor()) {
                error("Expected expression after '*' or '/'.");
                return false;
            }
        }
        return true;
    }

    /** factor -> NUMBER | IDENTIFIER | "(" expression ")" */
    private boolean parseFactor() {
        if (check(TokenType.NUMBER) || check(TokenType.IDENTIFIER)) {
            advance();
            return true;
        }

        if (match(TokenType.LEFT_PAREN)) {
            if (!parseExpression()) {
                error("Expected expression after '('.");
                return false;
            }
            if (!match(TokenType.RIGHT_PAREN)) {
                error("Expected ')' to close expression.");
                return false;
            }
            return true;
        }

        return false; // not a valid factor start
    }

    // ---------- Helper methods ----------

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private void error(String message) {
        hadError = true;
        Token t = peek();
        System.out.println("Syntax Error (line " + t.line + "): " + message);
    }

    /** Skip tokens until we reach a likely statement boundary, so parsing can continue. */
    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;
            if (check(TokenType.NUM) || check(TokenType.PRINT)) return;
            advance();
        }
    }

    // Tracks whether ANY error occurred across the whole program.
    private boolean overallFailed = false;
}
