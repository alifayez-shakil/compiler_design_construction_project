package compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive Descent Parser for the language.
 *
 * Grammar:
 *   program      -> statement* EOF
 *   statement    -> varDecl | assignment | printStmt | ifStmt | whileStmt | block
 *   varDecl      -> ("gona" | "doshomik") IDENTIFIER ":=" expression ";"
 *   assignment   -> IDENTIFIER ":=" expression ";"
 *   printStmt    -> "koiyo" "(" expression ")" ";"
 *   ifStmt       -> "jodi" "(" expression ")" block ("nohoile" block)?
 *   whileStmt    -> "jotokhon" "(" expression ")" block
 *   block        -> "{" statement* "}"
 *
 *   expression   -> equality
 *   equality     -> comparison (("==" | "!=") comparison)*
 *   comparison   -> term ((">" | ">=" | "<" | "<=") term)*
 *   term         -> factor (("+" | "-") factor)*
 *   factor       -> unary (("*" | "/" | "%") unary)*
 *   unary        -> ("+" | "-") unary | primary
 *   primary      -> NUMBER | IDENTIFIER | "(" expression ")"
 */
public class Parser {
    private final List<Token> tokens;
    private final Diagnostic diagnostics;
    private int current = 0;
    private boolean hadError = false;   // error during the CURRENT statement
    private boolean overallFailed = false;

    public Parser(List<Token> tokens, Diagnostic diagnostics) {
        this.tokens = tokens;
        this.diagnostics = diagnostics;
    }

    public Ast.Program parseProgram() {
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            hadError = false;
            Ast.Stmt stmt = parseStatement();
            if (hadError) {
                overallFailed = true;
                synchronize();
            } else if (stmt != null) {
                statements.add(stmt);
            }
        }
        return new Ast.Program(statements);
    }

    public boolean hasErrors() {
        return overallFailed;
    }

    // ---------- Statements ----------

    private Ast.Stmt parseStatement() {
        if (check(TokenType.GONA) || check(TokenType.DOSHOMIK)) return parseVarDecl();
        if (check(TokenType.IDENTIFIER)) return parseAssignment();
        if (check(TokenType.KOIYO)) return parsePrintStatement();
        if (check(TokenType.JODI)) return parseIfStatement();
        if (check(TokenType.JOTOKHON)) return parseWhileStatement();
        if (check(TokenType.LEFT_BRACE)) return parseBlock();

        error("Expected a statement but found '" + peek().lexeme + "'.");
        advance();
        return null;
    }

    private Ast.Stmt parseVarDecl() {
        int line = peek().line;
        String typeKeyword = peek().lexeme;
        advance(); // consume 'gona' or 'doshomik'

        if (!check(TokenType.IDENTIFIER)) {
            error("Expected identifier after '" + typeKeyword + "'.");
            return null;
        }
        String name = advance().lexeme;

        if (!match(TokenType.ASSIGN)) {
            error("Expected ':=' after identifier in declaration.");
            return null;
        }

        Ast.Expr initializer = parseExpression();
        if (initializer == null) {
            error("Expected expression after ':=' in declaration.");
            return null;
        }

        if (!match(TokenType.SEMICOLON)) {
            error("Expected ';' after declaration.");
            return null;
        }

        return new Ast.VarDecl(typeKeyword, name, initializer, line);
    }

    private Ast.Stmt parseAssignment() {
        int line = peek().line;
        String name = advance().lexeme; // consume identifier

        if (!match(TokenType.ASSIGN)) {
            error("Expected ':=' after identifier.");
            return null;
        }

        Ast.Expr value = parseExpression();
        if (value == null) {
            error("Expected expression after ':=' in assignment.");
            return null;
        }

        if (!match(TokenType.SEMICOLON)) {
            error("Expected ';' after assignment.");
            return null;
        }

        return new Ast.Assign(name, value, line);
    }

    private Ast.Stmt parsePrintStatement() {
        int line = peek().line;
        advance(); // consume 'koiyo'

        if (!match(TokenType.LEFT_PAREN)) {
            error("Expected '(' after 'koiyo'.");
            return null;
        }

        Ast.Expr value = parseExpression();
        if (value == null) {
            error("Expected expression inside koiyo(...).");
            return null;
        }

        if (!match(TokenType.RIGHT_PAREN)) {
            error("Expected ')' after expression in print statement.");
            return null;
        }

        if (!match(TokenType.SEMICOLON)) {
            error("Expected ';' after print statement.");
            return null;
        }

        return new Ast.Print(value, line);
    }

    private Ast.Stmt parseIfStatement() {
        int line = peek().line;
        advance(); // consume 'jodi'

        if (!match(TokenType.LEFT_PAREN)) {
            error("Expected '(' after 'jodi'.");
            return null;
        }

        Ast.Expr condition = parseExpression();
        if (condition == null) {
            error("Expected condition expression inside jodi(...).");
            return null;
        }

        if (!match(TokenType.RIGHT_PAREN)) {
            error("Expected ')' after condition.");
            return null;
        }

        if (!check(TokenType.LEFT_BRACE)) {
            error("Expected '{' to start jodi-block.");
            return null;
        }
        Ast.Block thenBranch = (Ast.Block) parseBlock();
        if (thenBranch == null) return null;

        Ast.Block elseBranch = null;
        if (match(TokenType.NOHOILE)) {
            if (!check(TokenType.LEFT_BRACE)) {
                error("Expected '{' to start nohoile-block.");
                return null;
            }
            elseBranch = (Ast.Block) parseBlock();
            if (elseBranch == null) return null;
        }

        return new Ast.If(condition, thenBranch, elseBranch, line);
    }

    private Ast.Stmt parseWhileStatement() {
        int line = peek().line;
        advance(); // consume 'jotokhon'

        if (!match(TokenType.LEFT_PAREN)) {
            error("Expected '(' after 'jotokhon'.");
            return null;
        }

        Ast.Expr condition = parseExpression();
        if (condition == null) {
            error("Expected condition expression inside jotokhon(...).");
            return null;
        }

        if (!match(TokenType.RIGHT_PAREN)) {
            error("Expected ')' after condition.");
            return null;
        }

        if (!check(TokenType.LEFT_BRACE)) {
            error("Expected '{' to start jotokhon-block.");
            return null;
        }
        Ast.Block body = (Ast.Block) parseBlock();
        if (body == null) return null;

        return new Ast.While(condition, body, line);
    }

    private Ast.Stmt parseBlock() {
        int line = peek().line;
        advance(); // consume '{'

        List<Ast.Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            Ast.Stmt stmt = parseStatement();
            if (hadError) {
                // Bail out of this block; parseProgram's synchronize() will
                // recover at the next top-level statement boundary.
                return null;
            }
            if (stmt != null) statements.add(stmt);
        }

        if (!match(TokenType.RIGHT_BRACE)) {
            error("Expected '}' to close block.");
            return null;
        }

        return new Ast.Block(statements, line);
    }

    // ---------- Expressions (precedence climbing) ----------

    private Ast.Expr parseExpression() {
        return parseEquality();
    }

    private Ast.Expr parseEquality() {
        Ast.Expr expr = parseComparison();
        if (expr == null) return null;

        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            Token op = advance();
            Ast.Expr right = parseComparison();
            if (right == null) { error("Expected expression after '" + op.lexeme + "'."); return null; }
            expr = new Ast.Binary(expr, op.type, right, op.line);
        }
        return expr;
    }

    private Ast.Expr parseComparison() {
        Ast.Expr expr = parseTerm();
        if (expr == null) return null;

        while (check(TokenType.GT) || check(TokenType.GE) || check(TokenType.LT) || check(TokenType.LE)) {
            Token op = advance();
            Ast.Expr right = parseTerm();
            if (right == null) { error("Expected expression after '" + op.lexeme + "'."); return null; }
            expr = new Ast.Binary(expr, op.type, right, op.line);
        }
        return expr;
    }

    private Ast.Expr parseTerm() {
        Ast.Expr expr = parseFactor();
        if (expr == null) return null;

        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            Ast.Expr right = parseFactor();
            if (right == null) { error("Expected expression after '" + op.lexeme + "'."); return null; }
            expr = new Ast.Binary(expr, op.type, right, op.line);
        }
        return expr;
    }

    private Ast.Expr parseFactor() {
        Ast.Expr expr = parseUnary();
        if (expr == null) return null;

        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            Token op = advance();
            Ast.Expr right = parseUnary();
            if (right == null) { error("Expected expression after '" + op.lexeme + "'."); return null; }
            expr = new Ast.Binary(expr, op.type, right, op.line);
        }
        return expr;
    }

    private Ast.Expr parseUnary() {
        if (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            Ast.Expr right = parseUnary();
            if (right == null) { error("Expected expression after unary '" + op.lexeme + "'."); return null; }
            return new Ast.Unary(op.type, right, op.line);
        }
        return parsePrimary();
    }

    private Ast.Expr parsePrimary() {
        if (check(TokenType.NUMBER)) {
            Token t = advance();
            boolean isDouble = t.lexeme.contains(".");
            return new Ast.Literal(t.lexeme, isDouble, t.line);
        }
        if (check(TokenType.IDENTIFIER)) {
            Token t = advance();
            return new Ast.Variable(t.lexeme, t.line);
        }
        if (match(TokenType.LEFT_PAREN)) {
            Ast.Expr inner = parseExpression();
            if (inner == null) {
                error("Expected expression after '('.");
                return null;
            }
            int line = previous().line;
            if (!match(TokenType.RIGHT_PAREN)) {
                error("Expected ')' to close expression.");
                return null;
            }
            return new Ast.Grouping(inner, line);
        }
        return null; // caller (equality/comparison/term/factor/unary) reports the specific error
    }

    // ---------- Helper methods ----------

    private boolean match(TokenType type) {
        if (check(type)) { advance(); return true; }
        return false;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
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
        diagnostics.error("Syntax", t.line, message);
    }

    /** Skip tokens until a likely statement boundary, so parsing can continue. */
    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON || previous().type == TokenType.RIGHT_BRACE) return;
            if (check(TokenType.GONA) || check(TokenType.DOSHOMIK) || check(TokenType.KOIYO)
                    || check(TokenType.JODI) || check(TokenType.JOTOKHON)) return;
            advance();
        }
    }
}
