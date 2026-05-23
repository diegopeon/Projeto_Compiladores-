package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import java.util.List;

/**
 * Bloco de instruções delimitado por chaves: { stmt* }
 *
 * Usado como corpo de if, else e while.
 * O bloco define um novo escopo de variáveis — informação que o
 * analisador semântico usará ao gerenciar a tabela de símbolos.
 */
public class BlockStmt extends Stmt {

    /** Instruções contidas neste bloco, em ordem de execução. */
    private final List<Stmt> statements;

    /**
     * @param statements lista de statements dentro do bloco
     */
    public BlockStmt(List<Stmt> statements) {
        this.statements = statements;
    }

    public List<Stmt> getStatements() {
        return statements;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
