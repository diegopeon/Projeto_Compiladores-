package compiler.ast.expr;

import compiler.ast.NodeVisitor;
import compiler.lexer.Token;

/**
 * Expressão unária: operador operando
 *
 * Suporta:
 *   !expr  — negação lógica (bool → bool)
 *   -expr  — negação aritmética (int → int)
 *
 * Exemplos:
 *   !ativo        →  UnaryExpr(op="!", operand=IdentifierExpr("ativo"))
 *   -42           →  UnaryExpr(op="-", operand=IntLiteralExpr(42))
 *   -(x + y)      →  UnaryExpr(op="-", operand=BinaryExpr(+, x, y))
 */
public class UnaryExpr extends Expr {

    /** Operando sobre o qual o operador é aplicado. */
    private final Expr operand;

    /**
     * @param operator token do operador (OP_NOT ou OP_MINUS)
     * @param operand  expressão sobre a qual o operador é aplicado
     */
    public UnaryExpr(Token operator, Expr operand) {
        super(operator);
        this.operand = operand;
    }

    public Token getOperator() { return getToken(); }
    public Expr  getOperand()  { return operand;    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitUnary(this);
    }
}
