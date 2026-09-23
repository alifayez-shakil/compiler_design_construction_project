package compiler;

import java.util.List;

public class Ast {

    // ===== Expressions =====
    public static abstract class Expr {
        public int line;
    }

    public static class Literal extends Expr {
        public final String text;     // raw lexeme, e.g. "10" or "3.14"
        public final boolean isDouble;
        public Literal(String text, boolean isDouble, int line) {
            this.text = text;
            this.isDouble = isDouble;
            this.line = line;
        }
    }

    public static class Variable extends Expr {
        public final String name;
        public Variable(String name, int line) {
            this.name = name;
            this.line = line;
        }
    }

    public static class Unary extends Expr {
        public final TokenType operator;
        public final Expr right;
        public Unary(TokenType operator, Expr right, int line) {
            this.operator = operator;
            this.right = right;
            this.line = line;
        }
    }

    public static class Binary extends Expr {
        public final Expr left;
        public final TokenType operator;
        public final Expr right;
        public Binary(Expr left, TokenType operator, Expr right, int line) {
            this.left = left;
            this.operator = operator;
            this.right = right;
            this.line = line;
        }
    }

    public static class Grouping extends Expr {
        public final Expr inner;
        public Grouping(Expr inner, int line) {
            this.inner = inner;
            this.line = line;
        }
    }

    // ===== Statements =====
    public static abstract class Stmt {
        public int line;
    }

    public static class VarDecl extends Stmt {
        public final String typeKeyword; // "gona" or "doshomik"
        public final String name;
        public final Expr initializer;
        public VarDecl(String typeKeyword, String name, Expr initializer, int line) {
            this.typeKeyword = typeKeyword;
            this.name = name;
            this.initializer = initializer;
            this.line = line;
        }
    }

    public static class Assign extends Stmt {
        public final String name;
        public final Expr value;
        public Assign(String name, Expr value, int line) {
            this.name = name;
            this.value = value;
            this.line = line;
        }
    }

    public static class Print extends Stmt {
        public final Expr value;
        public Print(Expr value, int line) {
            this.value = value;
            this.line = line;
        }
    }

    public static class Block extends Stmt {
        public final List<Stmt> statements;
        public Block(List<Stmt> statements, int line) {
            this.statements = statements;
            this.line = line;
        }
    }

    public static class If extends Stmt {
        public final Expr condition;
        public final Block thenBranch;
        public final Block elseBranch; // nullable
        public If(Expr condition, Block thenBranch, Block elseBranch, int line) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
            this.line = line;
        }
    }

    public static class While extends Stmt {
        public final Expr condition;
        public final Block body;
        public While(Expr condition, Block body, int line) {
            this.condition = condition;
            this.body = body;
            this.line = line;
        }
    }

    public static class Program {
        public final List<Stmt> statements;
        public Program(List<Stmt> statements) {
            this.statements = statements;
        }
    }
}
