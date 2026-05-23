package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.Expr;

/**
 * Laço de repetição: while ( condição ) bloco
 *
 * Executa o bloco repetidamente enquanto a condição for verdadeira.
 * A condição é verificada antes de cada iteração (pré-condicionado).
 * O analisador semântico verificará que a condição é do tipo bool.
 *
 * Exemplo:
 *   while (i <= 10) {
 *       soma = soma + i;
 *       i = i + 1;
 *   }
 */
public class WhileStmt extends Stmt {

    /** Expressão booleana avaliada a cada iteração. */
    private final Expr condition;

    /** Bloco de instruções repetido enquanto a condição for true. */
    private final BlockStmt body;

    /**
     * @param condition expressão de guarda do laço (deve ser bool)
     * @param body      bloco a executar em cada iteração
     */
    public WhileStmt(Expr condition, BlockStmt body) {
        this.condition = condition;
        this.body      = body;
    }

    public Expr      getCondition() { return condition; }
    public BlockStmt getBody()      { return body;      }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitWhile(this);
    }
}
