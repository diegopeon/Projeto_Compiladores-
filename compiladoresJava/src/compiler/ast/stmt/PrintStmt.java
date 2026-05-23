package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.Expr;

/**
 * Instrução de saída: print( expressão ) ;
 *
 * Avalia a expressão e exibe o resultado.
 * A expressão pode ser de qualquer tipo suportado (int, bool, string).
 */
public class PrintStmt extends Stmt {

    /** Expressão cujo valor será impresso. */
    private final Expr expression;

    /**
     * @param expression expressão a exibir
     */
    public PrintStmt(Expr expression) {
        this.expression = expression;
    }

    public Expr getExpression() {
        return expression;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitPrint(this);
    }
}
