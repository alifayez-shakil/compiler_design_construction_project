import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the AST (after semantic analysis) and emits equivalent Python code.
 * Keeps a small scope stack only to know when '/' should become '//' (int division).
 */
public class PythonCodeGenerator {
    private final StringBuilder out = new StringBuilder();
    private int indent = 0;
    private final List<Map<String, ValueType>> scopes = new ArrayList<>();

    public PythonCodeGenerator() {
        scopes.add(new HashMap<>());
    }

    public String generate(Ast.Program program) {
        for (Ast.Stmt stmt : program.statements) emitStmt(stmt);
        return out.toString();
    }

    // ---------- Statements ----------
    private void emitStmt(Ast.Stmt stmt) {
        if (stmt instanceof Ast.VarDecl)       emitVarDecl((Ast.VarDecl) stmt);
        else if (stmt instanceof Ast.Assign)   emitAssign((Ast.Assign) stmt);
        else if (stmt instanceof Ast.Print)    emitPrint((Ast.Print) stmt);
        else if (stmt instanceof Ast.If)       emitIf((Ast.If) stmt);
        else if (stmt instanceof Ast.While)    emitWhile((Ast.While) stmt);
        else if (stmt instanceof Ast.Block)    emitBlock((Ast.Block) stmt, true);
    }

    private void emitVarDecl(Ast.VarDecl decl) {
        ValueType t = decl.typeKeyword.equals("gona") ? ValueType.INT : ValueType.DOUBLE;
        currentScope().put(decl.name, t);
        writeLine(decl.name + " = " + emitExpr(decl.initializer));
    }

    private void emitAssign(Ast.Assign assign) {
        writeLine(assign.name + " = " + emitExpr(assign.value));
    }

    private void emitPrint(Ast.Print print) {
        writeLine("print(" + emitExpr(print.value) + ")");
    }

    private void emitIf(Ast.If ifStmt) {
        writeLine("if " + emitExpr(ifStmt.condition) + ":");
        emitBlock(ifStmt.thenBranch, true);
        if (ifStmt.elseBranch != null) {
            writeLine("else:");
            emitBlock(ifStmt.elseBranch, true);
        }
    }

    private void emitWhile(Ast.While whileStmt) {
        writeLine("while " + emitExpr(whileStmt.condition) + ":");
        emitBlock(whileStmt.body, true);
    }

    private void emitBlock(Ast.Block block, boolean newScope) {
        if (newScope) scopes.add(new HashMap<>());
        indent++;
        if (block.statements.isEmpty()) writeLine("pass");
        for (Ast.Stmt s : block.statements) emitStmt(s);
        indent--;
        if (newScope) scopes.remove(scopes.size() - 1);
    }

    // ---------- Expressions ----------
    private String emitExpr(Ast.Expr expr) {
        if (expr instanceof Ast.Literal)  return ((Ast.Literal) expr).text;
        if (expr instanceof Ast.Variable) return ((Ast.Variable) expr).name;
        if (expr instanceof Ast.Grouping) return "(" + emitExpr(((Ast.Grouping) expr).inner) + ")";
        if (expr instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) expr;
            String op = (u.operator == TokenType.MINUS) ? "-" : "+";
            return op + emitExpr(u.right);
        }
        if (expr instanceof Ast.Binary)   return emitBinary((Ast.Binary) expr);
        return "None";
    }

    private String emitBinary(Ast.Binary bin) {
        String left = emitExpr(bin.left);
        String right = emitExpr(bin.right);
        String op = symbol(bin.operator);

        // Integer division: if both operands are INT and op is '/', emit '//'
        if (bin.operator == TokenType.SLASH) {
            ValueType lt = typeOf(bin.left);
            ValueType rt = typeOf(bin.right);
            if (lt == ValueType.INT && rt == ValueType.INT) op = "//";
        }
        return "(" + left + " " + op + " " + right + ")";
    }

    // ---------- Type tracking (only for correct '/') ----------
    private ValueType typeOf(Ast.Expr expr) {
        if (expr instanceof Ast.Literal)  return ((Ast.Literal) expr).isDouble ? ValueType.DOUBLE : ValueType.INT;
        if (expr instanceof Ast.Variable) return lookup(((Ast.Variable) expr).name);
        if (expr instanceof Ast.Grouping) return typeOf(((Ast.Grouping) expr).inner);
        if (expr instanceof Ast.Unary)    return typeOf(((Ast.Unary) expr).right);
        if (expr instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary) expr;
            if (isComparison(b.operator)) return ValueType.BOOLEAN;
            ValueType lt = typeOf(b.left), rt = typeOf(b.right);
            return (lt == ValueType.DOUBLE || rt == ValueType.DOUBLE) ? ValueType.DOUBLE : ValueType.INT;
        }
        return ValueType.ERROR;
    }

    private boolean isComparison(TokenType t) {
        return t == TokenType.GT || t == TokenType.GE || t == TokenType.LT
                || t == TokenType.LE || t == TokenType.EQ || t == TokenType.NEQ;
    }

    private ValueType lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--)
            if (scopes.get(i).containsKey(name)) return scopes.get(i).get(name);
        return ValueType.ERROR;
    }

    private Map<String, ValueType> currentScope() {
        return scopes.get(scopes.size() - 1);
    }

    private void writeLine(String s) {
        for (int i = 0; i < indent; i++) out.append("    ");
        out.append(s).append("\n");
    }

    private String symbol(TokenType t) {
        switch (t) {
            case PLUS: return "+";
            case MINUS: return "-";
            case STAR: return "*";
            case SLASH: return "/";
            case PERCENT: return "%";
            case GT: return ">";
            case GE: return ">=";
            case LT: return "<";
            case LE: return "<=";
            case EQ: return "==";
            case NEQ: return "!=";
            default: return "?";
        }
    }
}