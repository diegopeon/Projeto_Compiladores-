package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.Expr;
import compiler.lexer.Token;

/**
 * Atribuição de variável: nome = expressão ;
 *
 * Diferente de uma declaração, aqui a variável já existe.
 * O analisador semântico verificará se ela foi declarada e se
 * o tipo da expressão é compatível com o tipo declarado.
 *
 * Exemplo: x = x + 1;
 */
public class AssignStmt extends Stmt {

    /** Token com o nome da variável que receberá o valor. */
    private final Token nameToken;

    /** Expressão cujo resultado será atribuído à variável. */
    private final Expr value;

    /**
     * @param nameToken token do identificador alvo da atribuição
     * @param value     expressão com o novo valor
     */
    public AssignStmt(Token nameToken, Expr value) {
        this.nameToken = nameToken;
        this.value     = value;
    }

    public Token getNameToken() { return nameToken; }
    public Expr  getValue()     { return value;     }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitAssign(this);
    }
}
