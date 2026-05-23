package compiler.parser;

/**
 * Exceção lançada pelo Parser ao encontrar um erro sintático.
 *
 * Carrega uma mensagem descritiva com linha, coluna e contexto do erro,
 * permitindo ao usuário localizar exatamente onde o programa está incorreto.
 *
 * É uma RuntimeException (unchecked) para não poluir as assinaturas dos
 * métodos recursivos do parser com cláusulas throws.
 */
public class ParseException extends RuntimeException {

    /**
     * @param message descrição do erro (inclui linha e coluna)
     */
    public ParseException(String message) {
        super(message);
    }
}
