package compiler.ast.expr;

import compiler.ast.Node;
import compiler.lexer.Token;

/**
 * Classe base abstrata para todos os nós de expressão.
 *
 * Uma expressão é um trecho de código que é avaliado e produz um valor.
 * Exemplos: "2 + 3", "x > 0", "true", "nome"
 *
 * Armazenamos o token de origem para que as fases posteriores
 * (semântico, gerador de código) possam reportar erros com posição exata.
 */
public abstract class Expr implements Node {

    /**
     * Token do código-fonte que originou esta expressão.
     * Usado para rastrear a posição de erros semânticos.
     */
    private final Token token;

    /**
     * @param token token principal que representa este nó na fonte
     */
    protected Expr(Token token) {
        this.token = token;
    }

    /** @return o token de origem desta expressão */
    public Token getToken() {
        return token;
    }
}
