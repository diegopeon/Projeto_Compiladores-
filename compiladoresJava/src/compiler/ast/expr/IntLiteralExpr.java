package compiler.ast.expr;

import compiler.ast.NodeVisitor;
import compiler.lexer.Token;

/**
 * Literal inteiro: número
 *
 * Armazena o valor numérico já convertido de String para int,
 * evitando conversões repetidas nas fases posteriores.
 *
 * Exemplo: "42" → IntLiteralExpr(value=42)
 */
public class IntLiteralExpr extends Expr {

    /** Valor inteiro extraído do lexema do token. */
    private final int value;

    /**
     * @param token token INTEGER_LITERAL
     */
    public IntLiteralExpr(Token token) {
        super(token);
        this.value = Integer.parseInt(token.getLexeme());
    }

    public int getValue() {
        return value;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitIntLiteral(this);
    }
}
