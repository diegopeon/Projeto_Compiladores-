package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import java.util.List;

/**
 * Nó raiz da AST — representa o programa completo.
 *
 * Contém a lista ordenada de todos os statements de nível superior.
 * É sempre o ponto de entrada de qualquer visitor que percorra a árvore.
 */
public class ProgramNode extends Stmt {

    /** Lista de instruções do programa, na ordem em que aparecem no fonte. */
    private final List<Stmt> statements;

    /**
     * @param statements lista de statements do programa
     */
    public ProgramNode(List<Stmt> statements) {
        this.statements = statements;
    }

    public List<Stmt> getStatements() {
        return statements;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitProgram(this);
    }
}
