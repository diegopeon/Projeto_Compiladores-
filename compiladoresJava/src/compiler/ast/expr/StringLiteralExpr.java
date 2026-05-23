package compiler.ast.expr;

import compiler.ast.NodeVisitor;
import compiler.lexer.Token;

/**
 * Literal de string: "texto"
 *
 * O lexema armazenado no token já é o conteúdo sem as aspas,
 * pois o Lexer as descarta durante a leitura.
 *
 * Exemplo: "olá" → StringLiteralExpr(value="olá")
 */
public class StringLiteralExpr extends Expr {

    /**
     * @param token token STRING_LITERAL (lexema sem aspas)
     */
    public StringLiteralExpr(Token token) {
        super(token);
    }

    /** @return o conteúdo da string sem as aspas delimitadoras */
    public String getValue() {
        return getToken().getLexeme();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitStringLiteral(this);
    }
}
