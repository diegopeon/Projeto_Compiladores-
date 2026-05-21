package compiler.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analisador Léxico (Scanner / Lexer) da linguagem.
 *
 * Responsabilidade única: transformar uma string de código-fonte em uma
 * lista sequencial de {@link Token}s, descartando espaços em branco e
 * comentários no processo.
 *
 * Teoria aplicada:
 *   Cada método de reconhecimento (scanNumber, scanIdentifier, etc.) simula
 *   um Autômato Finito Determinístico (AFD) para a expressão regular
 *   correspondente. O método {@link #nextToken()} age como o AFD principal
 *   que despacha para os AFDs especializados a partir do primeiro caractere.
 *
 * Estratégia de leitura:
 *   Mantemos dois ponteiros no texto:
 *     - {@code current}: posição do próximo caractere a ser lido ("lookahead").
 *     - {@code start}  : início do lexema em construção.
 *   Quando um token é finalizado, {@code start} é atualizado para {@code current}.
 */
public class Lexer {

    // -------------------------------------------------------------------------
    // Tabela de palavras reservadas — mapeamento direto lexema → tipo
    // -------------------------------------------------------------------------

    /**
     * Mapa estático e imutável das palavras reservadas da linguagem.
     * Inicializado uma única vez ao carregar a classe (static initializer).
     */
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("int",    TokenType.KW_INT);
        KEYWORDS.put("bool",   TokenType.KW_BOOL);
        KEYWORDS.put("string", TokenType.KW_STRING);
        KEYWORDS.put("if",     TokenType.KW_IF);
        KEYWORDS.put("else",   TokenType.KW_ELSE);
        KEYWORDS.put("while",  TokenType.KW_WHILE);
        KEYWORDS.put("print",  TokenType.KW_PRINT);
        KEYWORDS.put("read",   TokenType.KW_READ);
        KEYWORDS.put("true",   TokenType.KW_TRUE);
        KEYWORDS.put("false",  TokenType.KW_FALSE);
    }

    // -------------------------------------------------------------------------
    // Estado interno do lexer
    // -------------------------------------------------------------------------

    /** Código-fonte completo como array de caracteres para acesso indexado. */
    private final char[] source;

    /** Índice do próximo caractere a ser consumido. */
    private int current;

    /** Linha atual no código-fonte (começa em 1). */
    private int line;

    /** Coluna atual no código-fonte (começa em 1). */
    private int column;

    /** Lista de erros léxicos encontrados (coletamos todos antes de abortar). */
    private final List<String> errors;

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------

    /**
     * Inicializa o lexer com o código-fonte a ser analisado.
     *
     * @param source código-fonte completo como string
     */
    public Lexer(String source) {
        this.source  = source.toCharArray();
        this.current = 0;
        this.line    = 1;
        this.column  = 1;
        this.errors  = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Realiza a tokenização completa do código-fonte.
     *
     * Percorre todo o source e produz a lista de tokens, finalizando sempre
     * com um token EOF. Após a chamada, {@link #getErrors()} pode ser
     * consultado para verificar erros léxicos encontrados.
     *
     * @return lista de tokens na ordem em que aparecem no código
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        Token token;
        do {
            token = nextToken();
            // Tokens UNKNOWN já tiveram o erro registrado em errors;
            // ainda os adicionamos para que o parser possa continuar.
            tokens.add(token);
        } while (!token.isType(TokenType.EOF));

        return tokens;
    }

    /**
     * Retorna a lista de erros léxicos encontrados durante a tokenização.
     * Deve ser consultado após {@link #tokenize()}.
     *
     * @return lista (possivelmente vazia) de mensagens de erro
     */
    public List<String> getErrors() {
        return errors;
    }

    /** @return true se nenhum erro léxico foi encontrado */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Máquina de estados principal
    // -------------------------------------------------------------------------

    /**
     * Reconhece e retorna o próximo token do código-fonte.
     *
     * Este é o "despachante" principal: pula whitespace/comentários e,
     * a partir do primeiro caractere significativo, delega para o
     * reconhecedor específico.
     *
     * @return o próximo {@link Token} reconhecido
     */
    private Token nextToken() {

        // Descarta espaços em branco e comentários antes de começar
        skipWhitespaceAndComments();

        // Fim do arquivo?
        if (isAtEnd()) {
            return makeToken(TokenType.EOF, "<EOF>", line, column);
        }

        // Marcamos o início deste lexema para calcular o comprimento depois
        int tokenLine   = line;
        int tokenColumn = column;
        char c = advance();  // consome o primeiro caractere

        // ---- Números inteiros ------------------------------------------------
        if (Character.isDigit(c)) {
            return scanNumber(c, tokenLine, tokenColumn);
        }

        // ---- Identificadores e palavras reservadas ---------------------------
        if (Character.isLetter(c) || c == '_') {
            return scanIdentifierOrKeyword(c, tokenLine, tokenColumn);
        }

        // ---- Strings literais ------------------------------------------------
        if (c == '"') {
            return scanString(tokenLine, tokenColumn);
        }

        // ---- Operadores e delimitadores (switch determinístico) --------------
        switch (c) {

            // Operadores aritméticos simples
            case '+': return makeToken(TokenType.OP_PLUS,     "+", tokenLine, tokenColumn);
            case '-': return makeToken(TokenType.OP_MINUS,    "-", tokenLine, tokenColumn);
            case '*': return makeToken(TokenType.OP_MULTIPLY, "*", tokenLine, tokenColumn);
            case '/': return makeToken(TokenType.OP_DIVIDE,   "/", tokenLine, tokenColumn);

            // Operadores que podem ser simples ou compostos (ex: = vs ==)
            case '=': return matchNext('=')
                    ? makeToken(TokenType.OP_EQUAL,  "==", tokenLine, tokenColumn)
                    : makeToken(TokenType.OP_ASSIGN, "=",  tokenLine, tokenColumn);

            case '!': return matchNext('=')
                    ? makeToken(TokenType.OP_NOT_EQUAL, "!=", tokenLine, tokenColumn)
                    : makeToken(TokenType.OP_NOT,       "!",  tokenLine, tokenColumn);

            case '<': return matchNext('=')
                    ? makeToken(TokenType.OP_LESS_EQUAL, "<=", tokenLine, tokenColumn)
                    : makeToken(TokenType.OP_LESS_THAN,  "<",  tokenLine, tokenColumn);

            case '>': return matchNext('=')
                    ? makeToken(TokenType.OP_GREATER_EQUAL, ">=", tokenLine, tokenColumn)
                    : makeToken(TokenType.OP_GREATER_THAN,  ">",  tokenLine, tokenColumn);

            case '&': {
                // && — AND lógico; '&' sozinho não é válido na linguagem
                if (matchNext('&')) {
                    return makeToken(TokenType.OP_AND, "&&", tokenLine, tokenColumn);
                }
                return unknownToken("&", tokenLine, tokenColumn);
            }

            case '|': {
                // || — OR lógico; '|' sozinho não é válido na linguagem
                if (matchNext('|')) {
                    return makeToken(TokenType.OP_OR, "||", tokenLine, tokenColumn);
                }
                return unknownToken("|", tokenLine, tokenColumn);
            }

            // Delimitadores
            case '(': return makeToken(TokenType.LPAREN,    "(", tokenLine, tokenColumn);
            case ')': return makeToken(TokenType.RPAREN,    ")", tokenLine, tokenColumn);
            case '{': return makeToken(TokenType.LBRACE,    "{", tokenLine, tokenColumn);
            case '}': return makeToken(TokenType.RBRACE,    "}", tokenLine, tokenColumn);
            case ';': return makeToken(TokenType.SEMICOLON, ";", tokenLine, tokenColumn);
            case ',': return makeToken(TokenType.COMMA,     ",", tokenLine, tokenColumn);

            // Caractere não reconhecido pela linguagem
            default:
                return unknownToken(String.valueOf(c), tokenLine, tokenColumn);
        }
    }

    // -------------------------------------------------------------------------
    // Reconhecedores específicos (AFDs individuais)
    // -------------------------------------------------------------------------

    /**
     * AFD para reconhecimento de inteiros: [0-9]+
     *
     * O primeiro dígito {@code firstDigit} já foi consumido por {@code advance()}.
     * Continuamos consumindo enquanto houver dígitos consecutivos.
     */
    private Token scanNumber(char firstDigit, int tokenLine, int tokenColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append(firstDigit);

        // Continua enquanto o próximo caractere for um dígito
        while (!isAtEnd() && Character.isDigit(peek())) {
            sb.append(advance());
        }

        return makeToken(TokenType.INTEGER_LITERAL, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * AFD para identificadores e palavras reservadas: [a-zA-Z_][a-zA-Z0-9_]*
     *
     * Após coletar o lexema completo, consultamos a tabela {@link #KEYWORDS}
     * para distinguir palavras reservadas de identificadores do usuário.
     */
    private Token scanIdentifierOrKeyword(char firstChar, int tokenLine, int tokenColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);

        // Consome letras, dígitos e underscore
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }

        String lexeme = sb.toString();

        // Verifica se é uma palavra reservada; se não for, é um identificador
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);

        return makeToken(type, lexeme, tokenLine, tokenColumn);
    }

    /**
     * AFD para strings literais: "([^"\n])*"
     *
     * A aspas de abertura já foi consumida antes desta chamada.
     * Consumimos até encontrar a aspas de fechamento ou fim de linha/arquivo
     * (que indicam uma string não-terminada — erro léxico).
     */
    private Token scanString(int tokenLine, int tokenColumn) {
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd() && peek() != '"' && peek() != '\n') {
            sb.append(advance());
        }

        if (isAtEnd() || peek() == '\n') {
            // Erro: string não foi fechada antes do fim da linha / arquivo
            String errorMsg = String.format(
                "Erro léxico [%d:%d]: string não terminada \"%s",
                tokenLine, tokenColumn, sb
            );
            errors.add(errorMsg);
            return makeToken(TokenType.UNKNOWN, sb.toString(), tokenLine, tokenColumn);
        }

        advance(); // consome a aspas de fechamento '"'
        return makeToken(TokenType.STRING_LITERAL, sb.toString(), tokenLine, tokenColumn);
    }

    // -------------------------------------------------------------------------
    // Tratamento de espaços e comentários
    // -------------------------------------------------------------------------

    /**
     * Avança o cursor enquanto encontrar whitespace ou comentários.
     *
     * Suporta dois estilos de comentário:
     *   - Linha:  // até o fim da linha
     *   - Bloco:  /* ... * /  (multi-linha)
     */
    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();

            // Espaço, tab, retorno de carro
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();

            // Nova linha — incrementamos o contador de linhas
            } else if (c == '\n') {
                advance();
                line++;
                column = 1;

            // Possível comentário — precisamos olhar o próximo caractere
            } else if (c == '/' && peekNext() == '/') {
                // Comentário de linha: consome até o fim da linha
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }

            } else if (c == '/' && peekNext() == '*') {
                // Comentário de bloco: consome até */
                skipBlockComment();

            } else {
                // Caractere significativo — para de pular
                break;
            }
        }
    }

    /**
     * Avança além de um comentário de bloco {@code /* ... *}{@code /}.
     * A barra inicial já está em {@code peek()} mas ainda não foi consumida.
     * Reporta erro se o comentário não for fechado antes do EOF.
     */
    private void skipBlockComment() {
        int startLine   = line;
        int startColumn = column;

        advance(); // consome '/'
        advance(); // consome '*'

        while (!isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 1;
                advance();
            } else if (peek() == '*' && peekNext() == '/') {
                advance(); // consome '*'
                advance(); // consome '/'
                return;    // comentário encerrado com sucesso
            } else {
                advance();
            }
        }

        // EOF antes de fechar o comentário
        errors.add(String.format(
            "Erro léxico [%d:%d]: comentário de bloco não encerrado",
            startLine, startColumn
        ));
    }

    // -------------------------------------------------------------------------
    // Primitivas de navegação no buffer de caracteres
    // -------------------------------------------------------------------------

    /**
     * Retorna o caractere atual SEM avançar o cursor (lookahead de 1).
     * Retorna '\0' se estiver no fim do arquivo.
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source[current];
    }

    /**
     * Retorna o caractere SEGUINTE ao atual SEM avançar o cursor (lookahead de 2).
     * Retorna '\0' se não houver próximo caractere.
     */
    private char peekNext() {
        if (current + 1 >= source.length) return '\0';
        return source[current + 1];
    }

    /**
     * Consome e retorna o caractere atual, avançando o cursor e a coluna.
     *
     * @return o caractere consumido
     */
    private char advance() {
        char c = source[current];
        current++;
        column++;
        return c;
    }

    /**
     * Consome o próximo caractere somente se ele for igual a {@code expected}.
     * Útil para reconhecer operadores compostos como {@code ==}, {@code !=}, etc.
     *
     * @param expected o caractere que esperamos encontrar
     * @return true se consumiu, false caso contrário
     */
    private boolean matchNext(char expected) {
        if (isAtEnd()) return false;
        if (source[current] != expected) return false;
        advance(); // consome o caractere esperado
        return true;
    }

    /** @return true se o cursor ultrapassou o fim do array de caracteres */
    private boolean isAtEnd() {
        return current >= source.length;
    }

    // -------------------------------------------------------------------------
    // Fábricas de token
    // -------------------------------------------------------------------------

    /**
     * Cria um token com os dados fornecidos.
     * Método centralizador para uniformizar a construção de tokens.
     */
    private Token makeToken(TokenType type, String lexeme, int line, int column) {
        return new Token(type, lexeme, line, column);
    }

    /**
     * Cria um token UNKNOWN e registra o erro correspondente.
     * Separado de {@link #makeToken} para deixar o código do switch mais limpo.
     */
    private Token unknownToken(String lexeme, int tokenLine, int tokenColumn) {
        errors.add(String.format(
            "Erro léxico [%d:%d]: caractere inesperado '%s'",
            tokenLine, tokenColumn, lexeme
        ));
        return makeToken(TokenType.UNKNOWN, lexeme, tokenLine, tokenColumn);
    }
}