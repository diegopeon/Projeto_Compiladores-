package compiler.lexer;

/**
 * Representa um token individual produzido pelo Lexer.
 *
 * Um token é a unidade mínima com significado léxico, análogo a uma
 * "palavra" em um idioma humano. Além do tipo e do valor textual,
 * armazenamos a posição no código-fonte para exibir mensagens de erro
 * precisas ao programador.
 *
 * É um Value Object: imutável após a criação (todos os campos são final).
 */
public class Token {

    /** Categoria léxica deste token (ex: INTEGER_LITERAL, KW_IF, OP_PLUS). */
    private final TokenType type;

    /**
     * Texto exato como aparece no código-fonte (lexema).
     * Ex: para um INTEGER_LITERAL, pode ser "42"; para KW_IF, é "if".
     */
    private final String lexeme;

    /** Número da linha onde o token começa (base 1, para mensagens de erro). */
    private final int line;

    /** Coluna onde o token começa (base 1, para mensagens de erro). */
    private final int column;

    /**
     * Constrói um token com todas as suas informações posicionais.
     *
     * @param type   o tipo léxico do token
     * @param lexeme o texto original do código-fonte
     * @param line   linha onde o token foi encontrado (começa em 1)
     * @param column coluna onde o token foi encontrado (começa em 1)
     */
    public Token(TokenType type, String lexeme, int line, int column) {
        this.type   = type;
        this.lexeme = lexeme;
        this.line   = line;
        this.column = column;
    }

    // -------------------------------------------------------------------------
    // Getters — acesso somente leitura aos campos imutáveis
    // -------------------------------------------------------------------------

    /** @return o tipo léxico deste token */
    public TokenType getType() {
        return type;
    }

    /** @return o lexema (texto original) deste token */
    public String getLexeme() {
        return lexeme;
    }

    /** @return linha onde este token aparece no código-fonte */
    public int getLine() {
        return line;
    }

    /** @return coluna onde este token aparece no código-fonte */
    public int getColumn() {
        return column;
    }

    // -------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------

    /**
     * Verifica se este token é de um determinado tipo.
     * Método auxiliar para tornar o código do parser mais legível.
     *
     * @param expected o tipo a verificar
     * @return true se o tipo deste token for igual a {@code expected}
     */
    public boolean isType(TokenType expected) {
        return this.type == expected;
    }

    /**
     * Retorna uma representação textual do token, útil para depuração.
     * Formato: [TIPO, "lexema", linha:coluna]
     * Exemplo: [KW_IF, "if", 3:5]
     */
    @Override
    public String toString() {
        return String.format("[%s, \"%s\", %d:%d]", type, lexeme, line, column);
    }
}