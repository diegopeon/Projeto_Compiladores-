package compiler.lexer;

/**
 * Enumeração de todos os tipos de tokens reconhecidos pela linguagem.
 *
 * Cada constante representa uma categoria léxica distinta.
 * Agrupamos por afinidade semântica para facilitar a leitura e manutenção.
 *
 * Teoria aplicada: cada tipo de token corresponde a uma expressão regular
 * reconhecida por um Autômato Finito Determinístico (AFD) interno ao Lexer.
 */
public enum TokenType {

    // -------------------------------------------------------------------------
    // LITERAIS — valores concretos escritos no código-fonte
    // -------------------------------------------------------------------------

    /** Número inteiro, ex: 0, 42, 1000 */
    INTEGER_LITERAL,

    /** Literal booleano: true ou false */
    BOOLEAN_LITERAL,

    /** Literal de string entre aspas duplas, ex: "hello" */
    STRING_LITERAL,

    // -------------------------------------------------------------------------
    // IDENTIFICADORES — nomes definidos pelo programador
    // -------------------------------------------------------------------------

    /** Nome de variável ou função definido pelo usuário, ex: idade, soma */
    IDENTIFIER,

    // -------------------------------------------------------------------------
    // PALAVRAS RESERVADAS — fazem parte da gramática da linguagem
    // -------------------------------------------------------------------------

    /** Tipo de dado inteiro: int */
    KW_INT,

    /** Tipo de dado booleano: bool */
    KW_BOOL,

    /** Tipo de dado string: string */
    KW_STRING,

    /** Condicional: if */
    KW_IF,

    /** Alternativa do condicional: else */
    KW_ELSE,

    /** Laço de repetição: while */
    KW_WHILE,

    /** Saída de dados: print */
    KW_PRINT,

    /** Entrada de dados: read */
    KW_READ,

    /** Valor lógico verdadeiro: true */
    KW_TRUE,

    /** Valor lógico falso: false */
    KW_FALSE,

    // -------------------------------------------------------------------------
    // OPERADORES ARITMÉTICOS
    // -------------------------------------------------------------------------

    /** Adição: + */
    OP_PLUS,

    /** Subtração: - */
    OP_MINUS,

    /** Multiplicação: * */
    OP_MULTIPLY,

    /** Divisão: / */
    OP_DIVIDE,

    // -------------------------------------------------------------------------
    // OPERADORES RELACIONAIS / LÓGICOS
    // -------------------------------------------------------------------------

    /** Igualdade: == */
    OP_EQUAL,

    /** Diferença: != */
    OP_NOT_EQUAL,

    /** Menor que: < */
    OP_LESS_THAN,

    /** Maior que: > */
    OP_GREATER_THAN,

    /** Menor ou igual: <= */
    OP_LESS_EQUAL,

    /** Maior ou igual: >= */
    OP_GREATER_EQUAL,

    /** Negação lógica: ! */
    OP_NOT,

    /** E lógico: && */
    OP_AND,

    /** Ou lógico: || */
    OP_OR,

    // -------------------------------------------------------------------------
    // OPERADOR DE ATRIBUIÇÃO
    // -------------------------------------------------------------------------

    /** Atribuição simples: = */
    OP_ASSIGN,

    // -------------------------------------------------------------------------
    // DELIMITADORES / PONTUAÇÃO
    // -------------------------------------------------------------------------

    /** Parêntese esquerdo: ( */
    LPAREN,

    /** Parêntese direito: ) */
    RPAREN,

    /** Chave esquerda: { */
    LBRACE,

    /** Chave direita: } */
    RBRACE,

    /** Ponto e vírgula: ; */
    SEMICOLON,

    /** Vírgula: , */
    COMMA,

    // -------------------------------------------------------------------------
    // TOKENS ESPECIAIS DE CONTROLE
    // -------------------------------------------------------------------------

    /** Fim do arquivo — indica que não há mais tokens a consumir */
    EOF,

    /**
     * Token inválido — caractere não reconhecido pela linguagem.
     * O lexer gera este token em vez de lançar uma exceção imediatamente,
     * permitindo que o parser colete múltiplos erros antes de abortar.
     */
    UNKNOWN
}