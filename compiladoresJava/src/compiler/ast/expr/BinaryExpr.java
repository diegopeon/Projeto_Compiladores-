package compiler.ast.expr;

import compiler.lexer.Token;

import compiler.ast.NodeVisitor;

/**
 * Expressão binária: left operador right
 *
 * Cobre todos os operadores de dois operandos:
 *   - Aritméticos:  +  -  *  /
 *   - Relacionais:  ==  !=  <  >  <=  >=
 *   - Lógicos:      &&  ||
 *
 * O token do operador é armazenado para que o analisador semântico
 * possa verificar a compatibilidade de tipos e para mensagens de erro.
 *
 * Exemplo: 2 + 3  →  BinaryExpr( left=IntLiteral(2), op="+", right=IntLiteral(3) )
 */
public class BinaryExpr extends Expr {

    /** Operando esquerdo. */
    private final Expr left;

    /**
     * Token do operador (ex: OP_PLUS, OP_EQUAL, OP_AND).
     * Herdado de Expr via {@link Expr#getToken()}.
     */

    /** Operando direito. */
    private final Expr right;

    /**
     * @param left     operando esquerdo
     * @param operator token do operador (define a operação e a posição)
     * @param right    operando direito
     */
    public BinaryExpr(Expr left, Token operator, Expr right) {
        super(operator); // o token do operador é o token "principal" desta expr
        this.left  = left;
        this.right = right;
    }

    public Expr  getLeft()     { return left;          }
    public Token getOperator() { return getToken();     }
    public Expr  getRight()    { return right;          }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitBinary(this);
    }
}
