package compiler.parser;

import compiler.ast.expr.*;
import compiler.ast.stmt.*;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Analisador Sintático (Parser) — Parser Descendente Recursivo.
 *
 * Recebe a lista de tokens produzida pelo Lexer e constrói a
 * Árvore de Sintaxe Abstrata (AST), verificando se o programa
 * respeita a gramática da linguagem.
 *
 * Cada método privado implementa uma regra da gramática:
 *
 *   program      → statement*  EOF
 *   statement    → varDecl | assign | ifStmt | whileStmt | print | read
 *   varDecl      → tipo IDENTIFIER ('=' expression)? ';'
 *   assign       → IDENTIFIER '=' expression ';'
 *   ifStmt       → 'if' '(' expression ')' block ('else' block)?
 *   whileStmt    → 'while' '(' expression ')' block
 *   print        → 'print' '(' expression ')' ';'
 *   read         → 'read'  '(' IDENTIFIER  ')' ';'
 *   block        → '{' statement* '}'
 *
 *   expression   → logicalOr
 *   logicalOr    → logicalAnd  ('||' logicalAnd)*
 *   logicalAnd   → equality    ('&&' equality)*
 *   equality     → relational  ('=='|'!=' relational)*
 *   relational   → additive    ('<'|'>'|'<='|'>=' additive)*
 *   additive     → multip      ('+'|'-' multip)*
 *   multip       → unary       ('*'|'/' unary)*
 *   unary        → ('!'|'-') unary | primary
 *   primary      → INTEGER | BOOL | STRING | IDENTIFIER | '(' expression ')'
 *
 * Estratégia de erros: ao encontrar um token inesperado, lança
 * {@link ParseException} com mensagem precisa (linha e coluna).
 * O modo pânico ("panic mode") pode ser adicionado futuramente para
 * continuar o parse após erros e coletar múltiplos problemas.
 */
public class Parser {

    // -------------------------------------------------------------------------
    // Conjunto de tokens que iniciam uma declaração de tipo
    // -------------------------------------------------------------------------

    /** Tokens que representam um tipo de dado da linguagem. */
    private static final Set<TokenType> TYPE_TOKENS = Set.of(
        TokenType.KW_INT,
        TokenType.KW_BOOL,
        TokenType.KW_STRING
    );

    // -------------------------------------------------------------------------
    // Estado interno
    // -------------------------------------------------------------------------

    /** Lista de tokens produzida pelo Lexer. */
    private final List<Token> tokens;

    /** Índice do token corrente (próximo a ser consumido). */
    private int current;

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------

    /**
     * @param tokens lista de tokens gerada pelo Lexer (deve terminar com EOF)
     */
    public Parser(List<Token> tokens) {
        this.tokens  = tokens;
        this.current = 0;
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Executa a análise sintática e retorna a raiz da AST.
     *
     * @return nó raiz {@link ProgramNode} com todos os statements do programa
     * @throws ParseException se a sequência de tokens violar a gramática
     */
    public ProgramNode parse() {
        List<Stmt> statements = new ArrayList<>();

        // Consome statements até chegar no fim do arquivo
        while (!isAtEnd()) {
            statements.add(parseStatement());
        }

        return new ProgramNode(statements);
    }

    // -------------------------------------------------------------------------
    // Regras de Statement
    // -------------------------------------------------------------------------

    /**
     * Despacha para o parser correto com base no token atual.
     *
     * statement → varDecl | assign | ifStmt | whileStmt | print | read
     */
    private Stmt parseStatement() {
        // Declaração de variável começa com um token de tipo (int, bool, string)
        if (isTypeToken(peek())) {
            return parseVarDecl();
        }

        // Atribuição começa com IDENTIFIER seguido de '='
        // Usamos lookahead para distinguir de um futuro identificador em expressão
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OP_ASSIGN)) {
            return parseAssign();
        }

        // Estruturas de controle e E/S
        if (check(TokenType.KW_IF))    return parseIf();
        if (check(TokenType.KW_WHILE)) return parseWhile();
        if (check(TokenType.KW_PRINT)) return parsePrint();
        if (check(TokenType.KW_READ))  return parseRead();

        // Token não reconhecido como início de statement
        Token bad = peek();
        throw new ParseException(String.format(
            "Erro sintático [%d:%d]: instrução inesperada '%s'",
            bad.getLine(), bad.getColumn(), bad.getLexeme()
        ));
    }

    /**
     * varDecl → tipo IDENTIFIER ('=' expression)? ';'
     */
    private VarDeclStmt parseVarDecl() {
        Token typeToken = advance();  // consome o token de tipo (int/bool/string)
        Token nameToken = consume(TokenType.IDENTIFIER, "Esperado nome de variável após o tipo");

        // Inicializador é opcional
        Expr initializer = null;
        if (match(TokenType.OP_ASSIGN)) {
            initializer = parseExpression();
        }

        consume(TokenType.SEMICOLON, "Esperado ';' após declaração de variável");
        return new VarDeclStmt(typeToken, nameToken, initializer);
    }

    /**
     * assign → IDENTIFIER '=' expression ';'
     */
    private AssignStmt parseAssign() {
        Token nameToken = advance();           // consome o IDENTIFIER
        consume(TokenType.OP_ASSIGN, "Esperado '=' na atribuição");
        Expr value = parseExpression();
        consume(TokenType.SEMICOLON, "Esperado ';' após atribuição");
        return new AssignStmt(nameToken, value);
    }

    /**
     * ifStmt → 'if' '(' expression ')' block ('else' block)?
     */
    private IfStmt parseIf() {
        consume(TokenType.KW_IF,  "Esperado 'if'");
        consume(TokenType.LPAREN, "Esperado '(' após 'if'");
        Expr condition = parseExpression();
        consume(TokenType.RPAREN, "Esperado ')' após condição do if");

        BlockStmt thenBranch = parseBlock();

        // O else é opcional — só consome se o próximo token for 'else'
        BlockStmt elseBranch = null;
        if (match(TokenType.KW_ELSE)) {
            elseBranch = parseBlock();
        }

        return new IfStmt(condition, thenBranch, elseBranch);
    }

    /**
     * whileStmt → 'while' '(' expression ')' block
     */
    private WhileStmt parseWhile() {
        consume(TokenType.KW_WHILE, "Esperado 'while'");
        consume(TokenType.LPAREN,   "Esperado '(' após 'while'");
        Expr condition = parseExpression();
        consume(TokenType.RPAREN,   "Esperado ')' após condição do while");

        BlockStmt body = parseBlock();
        return new WhileStmt(condition, body);
    }

    /**
     * print → 'print' '(' expression ')' ';'
     */
    private PrintStmt parsePrint() {
        consume(TokenType.KW_PRINT, "Esperado 'print'");
        consume(TokenType.LPAREN,   "Esperado '(' após 'print'");
        Expr expression = parseExpression();
        consume(TokenType.RPAREN,   "Esperado ')' após expressão do print");
        consume(TokenType.SEMICOLON,"Esperado ';' após print");
        return new PrintStmt(expression);
    }

    /**
     * read → 'read' '(' IDENTIFIER ')' ';'
     */
    private ReadStmt parseRead() {
        consume(TokenType.KW_READ,   "Esperado 'read'");
        consume(TokenType.LPAREN,    "Esperado '(' após 'read'");
        Token nameToken = consume(TokenType.IDENTIFIER, "Esperado nome de variável em 'read'");
        consume(TokenType.RPAREN,    "Esperado ')' após variável do read");
        consume(TokenType.SEMICOLON, "Esperado ';' após read");
        return new ReadStmt(nameToken);
    }

    /**
     * block → '{' statement* '}'
     */
    private BlockStmt parseBlock() {
        consume(TokenType.LBRACE, "Esperado '{' para iniciar bloco");
        List<Stmt> stmts = new ArrayList<>();

        // Consome statements até fechar a chave ou chegar no EOF
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(parseStatement());
        }

        consume(TokenType.RBRACE, "Esperado '}' para fechar bloco");
        return new BlockStmt(stmts);
    }

    // -------------------------------------------------------------------------
    // Regras de Expressão (ordem = precedência crescente)
    // -------------------------------------------------------------------------

    /**
     * expression → logicalOr
     *
     * Ponto de entrada para qualquer expressão.
     * A hierarquia de chamadas garante a precedência correta:
     * || tem menor precedência, primary tem maior.
     */
    private Expr parseExpression() {
        return parseLogicalOr();
    }

    /**
     * logicalOr → logicalAnd ('||' logicalAnd)*
     */
    private Expr parseLogicalOr() {
        Expr left = parseLogicalAnd();

        while (check(TokenType.OP_OR)) {
            Token operator = advance();
            Expr right = parseLogicalAnd();
            left = new BinaryExpr(left, operator, right);
        }

        return left;
    }

    /**
     * logicalAnd → equality ('&&' equality)*
     */
    private Expr parseLogicalAnd() {
        Expr left = parseEquality();

        while (check(TokenType.OP_AND)) {
            Token operator = advance();
            Expr right = parseEquality();
            left = new BinaryExpr(left, operator, right);
        }

        return left;
    }

    /**
     * equality → relational ('=='|'!=' relational)*
     */
    private Expr parseEquality() {
        Expr left = parseRelational();

        while (check(TokenType.OP_EQUAL) || check(TokenType.OP_NOT_EQUAL)) {
            Token operator = advance();
            Expr right = parseRelational();
            left = new BinaryExpr(left, operator, right);
        }

        return left;
    }

    /**
     * relational → additive ('<'|'>'|'<='|'>=' additive)*
     */
    private Expr parseRelational() {
        Expr left = parseAdditive();

        while (check(TokenType.OP_LESS_THAN)    ||
               check(TokenType.OP_GREATER_THAN)  ||
               check(TokenType.OP_LESS_EQUAL)    ||
               check(TokenType.OP_GREATER_EQUAL)) {
            Token operator = advance();
            Expr right = parseAdditive();
            left = new BinaryExpr(left, operator, right);
        }

        return left;
    }

    /**
     * additive → multip ('+'|'-' multip)*
     */
    private Expr parseAdditive() {
        Expr left = parseMultiplicative();

        while (check(TokenType.OP_PLUS) || check(TokenType.OP_MINUS)) {
            Token operator = advance();
            Expr right = parseMultiplicative();
            left = new BinaryExpr(left, operator, right);
        }

        return left;
    }

    /**
     * multip → unary ('*'|'/' unary)*
     */
    private Expr parseMultiplicative() {
        Expr left = parseUnary();

        while (check(TokenType.OP_MULTIPLY) || check(TokenType.OP_DIVIDE)) {
            Token operator = advance();
            Expr right = parseUnary();
            left = new BinaryExpr(left, operator, right);
        }

        return left;
    }

    /**
     * unary → ('!'|'-') unary | primary
     *
     * Recursivo para suportar casos como !!x ou --x.
     */
    private Expr parseUnary() {
        if (check(TokenType.OP_NOT) || check(TokenType.OP_MINUS)) {
            Token operator = advance();
            Expr operand = parseUnary();  // recursão para encadear unários
            return new UnaryExpr(operator, operand);
        }

        return parsePrimary();
    }

    /**
     * primary → INTEGER | BOOL | STRING | IDENTIFIER | '(' expression ')'
     *
     * Folhas da árvore de expressão — maior precedência.
     */
    private Expr parsePrimary() {
        // Literal inteiro
        if (check(TokenType.INTEGER_LITERAL)) {
            return new IntLiteralExpr(advance());
        }

        // Literais booleanos (true / false)
        if (check(TokenType.KW_TRUE) || check(TokenType.KW_FALSE)) {
            return new BoolLiteralExpr(advance());
        }

        // Literal de string
        if (check(TokenType.STRING_LITERAL)) {
            return new StringLiteralExpr(advance());
        }

        // Referência a variável
        if (check(TokenType.IDENTIFIER)) {
            return new IdentifierExpr(advance());
        }

        // Expressão parentesizada: '(' expr ')'
        if (check(TokenType.LPAREN)) {
            advance(); // consome '('
            Expr inner = parseExpression();
            consume(TokenType.RPAREN, "Esperado ')' após expressão");
            return inner;  // retorna a expressão interna sem envolver em nó próprio
        }

        // Nada reconhecível
        Token bad = peek();
        throw new ParseException(String.format(
            "Erro sintático [%d:%d]: expressão esperada, encontrado '%s'",
            bad.getLine(), bad.getColumn(), bad.getLexeme()
        ));
    }

    // -------------------------------------------------------------------------
    // Primitivas de navegação na lista de tokens
    // -------------------------------------------------------------------------

    /**
     * Retorna o token atual sem avançar (lookahead de 1).
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Retorna o token seguinte ao atual sem avançar (lookahead de 2).
     * Retorna o EOF se não houver próximo.
     */
    private Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 1);
    }

    /**
     * Consome e retorna o token atual, avançando o cursor.
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }

    /**
     * Verifica se o token atual é do tipo esperado, sem avançar.
     */
    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    /**
     * Verifica se o token SEGUINTE é do tipo esperado (lookahead de 2).
     * Usado para distinguir assign de expressão começando com identificador.
     */
    private boolean checkNext(TokenType type) {
        return peekNext().getType() == type;
    }

    /**
     * Consome o token atual se ele for do tipo esperado.
     * Retorna true se consumiu, false caso contrário.
     */
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * Consome o token atual exigindo que seja do tipo esperado.
     * Lança {@link ParseException} com mensagem descritiva se não for.
     *
     * @param type    tipo esperado
     * @param message mensagem de erro para o programador
     * @return o token consumido
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        Token bad = peek();
        throw new ParseException(String.format(
            "Erro sintático [%d:%d]: %s (encontrado '%s')",
            bad.getLine(), bad.getColumn(), message, bad.getLexeme()
        ));
    }

    /**
     * Verifica se o token atual é um token de tipo de dado.
     */
    private boolean isTypeToken(Token token) {
        return TYPE_TOKENS.contains(token.getType());
    }

    /**
     * @return true se chegamos ao token EOF
     */
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
}
