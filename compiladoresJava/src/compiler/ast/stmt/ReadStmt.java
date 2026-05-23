package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import compiler.lexer.Token;

/**
 * Instrução de entrada: read( nome ) ;
 *
 * Lê um valor do console e armazena na variável indicada.
 * O analisador semântico verificará que a variável foi declarada.
 *
 * Exemplo: read(n);
 */
public class ReadStmt extends Stmt {

    /** Token com o nome da variável que receberá o valor lido. */
    private final Token nameToken;

    /**
     * @param nameToken token do identificador alvo da leitura
     */
    public ReadStmt(Token nameToken) {
        this.nameToken = nameToken;
    }

    public Token getNameToken() {
        return nameToken;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitRead(this);
    }
}
