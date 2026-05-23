package compiler.semantic;

/**
 * Representa os tipos de dados suportados pela linguagem.
 *
 * Usado pela Tabela de Símbolos para registrar o tipo de cada variável
 * e pelo SemanticAnalyzer para verificar compatibilidade entre operandos.
 *
 * O tipo VOID é especial: representa ausência de valor — usado
 * internamente para statements que não produzem resultado
 * (ex: print, read, if, while).
 */
public enum DataType {

    /** Número inteiro: int */
    INT,

    /** Valor lógico: bool */
    BOOL,

    /** Cadeia de caracteres: string */
    STRING,

    /**
     * Ausência de valor — usado para statements.
     * Nenhuma variável pode ter este tipo.
     */
    VOID;

    /**
     * Converte o lexema do token de tipo para o DataType correspondente.
     * Ex: "int" → INT, "bool" → BOOL, "string" → STRING.
     *
     * @param lexeme lexema do token de tipo
     * @return o DataType correspondente
     * @throws IllegalArgumentException se o lexema não for um tipo válido
     */
    public static DataType fromLexeme(String lexeme) {
        return switch (lexeme) {
            case "int"    -> INT;
            case "bool"   -> BOOL;
            case "string" -> STRING;
            default -> throw new IllegalArgumentException(
                "Tipo desconhecido: " + lexeme
            );
        };
    }

    /**
     * Retorna o nome do tipo como aparece no código-fonte.
     * Útil para mensagens de erro legíveis.
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
