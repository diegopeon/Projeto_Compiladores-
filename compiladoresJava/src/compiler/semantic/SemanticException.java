package compiler.semantic;

/**
 * Exceção lançada pelo SemanticAnalyzer ao encontrar um erro semântico.
 *
 * Erros semânticos ocorrem quando o código é sintaticamente correto
 * mas não faz sentido logicamente. Exemplos:
 *   - Usar uma variável que não foi declarada
 *   - Declarar a mesma variável duas vezes no mesmo escopo
 *   - Somar um int com um bool
 *   - Usar uma expressão não-booleana como condição de if/while
 *
 * Assim como ParseException, é unchecked (RuntimeException) para
 * não poluir as assinaturas dos métodos do visitor.
 */
public class SemanticException extends RuntimeException {

    /**
     * @param message descrição do erro com linha e coluna
     */
    public SemanticException(String message) {
        super(message);
    }
}
