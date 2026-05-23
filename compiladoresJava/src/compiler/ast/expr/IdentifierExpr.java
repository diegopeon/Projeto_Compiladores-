package compiler.ast.expr;

import compiler.ast.NodeVisitor;
import compiler.lexer.Token;

/**
 * Referência a uma variável pelo nome: identificador
 *
 * O analisador semântico resolverá este nó consultando a Tabela de Símbolos
 * para verificar se a variável foi declarada e obter seu tipo.
 *
 * Exemplo: em "x + 1", o "x" gera um IdentifierExpr.
 */
public class IdentifierExpr extends Expr {

    /**
     * @param token token IDENTIFIER com o nome da variável
     */
    public IdentifierExpr(Token token) {
        super(token);
    }

    /** @return o nome da variável referenciada */
    public String getName() {
        return getToken().getLexeme();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitIdentifier(this);
    }
}
