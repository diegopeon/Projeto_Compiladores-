package compiler.ast.expr;

import compiler.ast.NodeVisitor;
import compiler.lexer.Token;

/**
 * Literal booleano: true | false
 *
 * Exemplo: "true" → BoolLiteralExpr(value=true)
 */
public class BoolLiteralExpr extends Expr {

    /** Valor booleano extraído do lexema. */
    private final boolean value;

    /**
     * @param token token KW_TRUE ou KW_FALSE
     */
    public BoolLiteralExpr(Token token) {
        super(token);
        this.value = token.getLexeme().equals("true");
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitBoolLiteral(this);
    }
}
