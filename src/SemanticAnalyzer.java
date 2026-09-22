import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the AST after parsing and checks:
 *  - each variable is declared before use, and not redeclared in the same scope
 *  - assignments/declarations match the declared type (gona=INT, doshomik=DOUBLE;
 *    an int value may widen into a doshomik variable, not the reverse)
 *  - jodi/jotokhon conditions are actual comparisons, not plain arithmetic
 *  - obvious divide/modulo-by-zero literals
 */
public class SemanticAnalyzer {
    private final List<Map<String, ValueType>> scopes = new ArrayList<>();
    private boolean hadError = false;

    public SemanticAnalyzer() {
        scopes.add(new HashMap<>()); // global scope
    }

    public boolean analyze(Ast.Program program) {
        for (Ast.Stmt stmt : program.statements) {
            checkStmt(stmt);
        }
        return !hadError;
    }

    // ---------- Statements ----------

    private void checkStmt(Ast.Stmt stmt) {
        if (stmt instanceof Ast.VarDecl) {
            checkVarDecl((Ast.VarDecl) stmt);
        } else if (stmt instanceof Ast.Assign) {
            checkAssign((Ast.Assign) stmt);
        } else if (stmt instanceof Ast.Print) {
            checkExpr(((Ast.Print) stmt).value);
        } else if (stmt instanceof Ast.If) {
            checkIf((Ast.If) stmt);
        } else if (stmt instanceof Ast.While) {
            checkWhile((Ast.While) stmt);
        } else if (stmt instanceof Ast.Block) {
            checkBlock((Ast.Block) stmt, true);
        }
    }

    private void checkVarDecl(Ast.VarDecl decl) {
        ValueType declaredType = decl.typeKeyword.equals("gona") ? ValueType.INT : ValueType.DOUBLE;
        ValueType exprType = checkExpr(decl.initializer);

        if (currentScope().containsKey(decl.name)) {
            error(decl.line, "'" + decl.name + "' is already declared in this scope.");
        } else {
            currentScope().put(decl.name, declaredType);
        }

        if (exprType != ValueType.ERROR && !isAssignable(declaredType, exprType)) {
            error(decl.line, "Cannot assign a " + exprType + " value to " + decl.typeKeyword
                    + " variable '" + decl.name + "'.");
        }
    }

    private void checkAssign(Ast.Assign assign) {
        ValueType declaredType = lookup(assign.name);
        ValueType exprType = checkExpr(assign.value);

        if (declaredType == null) {
            error(assign.line, "Variable '" + assign.name + "' is used before it was declared.");
            return;
        }

        if (exprType != ValueType.ERROR && !isAssignable(declaredType, exprType)) {
            error(assign.line, "Cannot assign a " + exprType + " value to variable '"
                    + assign.name + "' of type " + declaredType + ".");
        }
    }

    private void checkIf(Ast.If ifStmt) {
        ValueType condType = checkExpr(ifStmt.condition);
        if (condType != ValueType.BOOLEAN && condType != ValueType.ERROR) {
            error(ifStmt.line, "Condition inside jodi(...) must be a comparison, e.g. x < 10.");
        }
        checkBlock(ifStmt.thenBranch, true);
        if (ifStmt.elseBranch != null) checkBlock(ifStmt.elseBranch, true);
    }

    private void checkWhile(Ast.While whileStmt) {
        ValueType condType = checkExpr(whileStmt.condition);
        if (condType != ValueType.BOOLEAN && condType != ValueType.ERROR) {
            error(whileStmt.line, "Condition inside jotokhon(...) must be a comparison, e.g. x < 10.");
        }
        checkBlock(whileStmt.body, true);
    }

    private void checkBlock(Ast.Block block, boolean newScope) {
        if (newScope) scopes.add(new HashMap<>());
        for (Ast.Stmt stmt : block.statements) {
            checkStmt(stmt);
        }
        if (newScope) scopes.remove(scopes.size() - 1);
    }

    // ---------- Expressions ----------

    private ValueType checkExpr(Ast.Expr expr) {
        if (expr instanceof Ast.Literal) {
            return ((Ast.Literal) expr).isDouble ? ValueType.DOUBLE : ValueType.INT;
        }
        if (expr instanceof Ast.Variable) {
            String name = ((Ast.Variable) expr).name;
            ValueType type = lookup(name);
            if (type == null) {
                error(expr.line, "Variable '" + name + "' is used before it was declared.");
                return ValueType.ERROR;
            }
            return type;
        }
        if (expr instanceof Ast.Grouping) {
            return checkExpr(((Ast.Grouping) expr).inner);
        }
        if (expr instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) expr;
            ValueType right = checkExpr(u.right);
            if (right == ValueType.BOOLEAN) {
                error(u.line, "Unary '" + symbol(u.operator) + "' cannot be applied to a comparison result.");
                return ValueType.ERROR;
            }
            return right;
        }
        if (expr instanceof Ast.Binary) {
            return checkBinary((Ast.Binary) expr);
        }
        return ValueType.ERROR;
    }

    private ValueType checkBinary(Ast.Binary bin) {
        ValueType left = checkExpr(bin.left);
        ValueType right = checkExpr(bin.right);
        if (left == ValueType.ERROR || right == ValueType.ERROR) return ValueType.ERROR;

        boolean isComparison = bin.operator == TokenType.GT || bin.operator == TokenType.GE
                || bin.operator == TokenType.LT || bin.operator == TokenType.LE
                || bin.operator == TokenType.EQ || bin.operator == TokenType.NEQ;

        if (isComparison) {
            if (left == ValueType.BOOLEAN || right == ValueType.BOOLEAN) {
                error(bin.line, "Cannot compare a comparison result with '" + symbol(bin.operator) + "'.");
                return ValueType.ERROR;
            }
            return ValueType.BOOLEAN;
        }

        // Arithmetic operator
        if (left == ValueType.BOOLEAN || right == ValueType.BOOLEAN) {
            error(bin.line, "Arithmetic operator '" + symbol(bin.operator)
                    + "' cannot be used on a comparison result.");
            return ValueType.ERROR;
        }

        if (bin.operator == TokenType.PERCENT && (left == ValueType.DOUBLE || right == ValueType.DOUBLE)) {
            error(bin.line, "'%' requires gona (integer) operands, not doshomik.");
            return ValueType.ERROR;
        }

        if ((bin.operator == TokenType.SLASH || bin.operator == TokenType.PERCENT) && isZeroLiteral(bin.right)) {
            error(bin.line, "Division or modulo by zero.");
            return ValueType.ERROR;
        }

        // int + int -> int; anything involving a doshomik -> doshomik
        return (left == ValueType.DOUBLE || right == ValueType.DOUBLE) ? ValueType.DOUBLE : ValueType.INT;
    }

    private boolean isZeroLiteral(Ast.Expr expr) {
        if (expr instanceof Ast.Literal) {
            try {
                return Double.parseDouble(((Ast.Literal) expr).text) == 0.0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    // ---------- Helpers ----------

    private boolean isAssignable(ValueType target, ValueType value) {
        if (target == value) return true;
        return target == ValueType.DOUBLE && value == ValueType.INT; // int widens into doshomik
    }

    private String symbol(TokenType type) {
        switch (type) {
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
            default: return type.toString();
        }
    }

    private ValueType lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) return scopes.get(i).get(name);
        }
        return null;
    }

    private Map<String, ValueType> currentScope() {
        return scopes.get(scopes.size() - 1);
    }

    private void error(int line, String message) {
        hadError = true;
        System.out.println("Semantic Error (line " + line + "): " + message);
    }
}
