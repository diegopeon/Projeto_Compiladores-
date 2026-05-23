package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.Expr;

/**
 * Condicional: if ( condição ) bloco [ else bloco ]
 *
 * O ramo else é opcional — quando ausente, {@link #elseBranch} é null.
 * A condição deve ser do tipo bool (verificado pelo analisador semântico).
 *
 * Exemplos:
 *   if (x > 0) { print("positivo"); }
 *   if (ativo) { ... } else { ... }
 */
public class IfStmt extends Stmt {

    /** Expressão booleana que decide qual ramo executar. */
    private final Expr condition;

    /** Bloco executado quando a condição for verdadeira. */
    private final BlockStmt thenBranch;

    /**
     * Bloco executado quando a condição for falsa, ou null se não houver else.
     */
    private final BlockStmt elseBranch;

    /**
     * @param condition  expressão de condição (deve ser bool)
     * @param thenBranch bloco do "then"
     * @param elseBranch bloco do "else", ou null
     */
    public IfStmt(Expr condition, BlockStmt thenBranch, BlockStmt elseBranch) {
        this.condition  = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Expr      getCondition()  { return condition;  }
    public BlockStmt getThenBranch() { return thenBranch; }
    public BlockStmt getElseBranch() { return elseBranch; }

    /** @return true se este if possui um ramo else */
    public boolean hasElse() {
        return elseBranch != null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitIf(this);
    }
}
