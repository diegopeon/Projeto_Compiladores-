package compiler.semantic;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.*;
import compiler.ast.stmt.*;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

/**
 * Analisador Semântico — Etapa 3 do compilador.
 *
 * Implementa NodeVisitor<DataType>: percorre toda a AST e retorna
 * o tipo de dado de cada nó. Para statements, retorna VOID.
 * Para expressões, retorna o tipo inferido (INT, BOOL ou STRING).
 *
 * Verificações realizadas:
 *
 *   1. Variável não declarada
 *      → usar "x" sem ter feito "int x" antes
 *
 *   2. Variável declarada duas vezes no mesmo escopo
 *      → "int x = 1; int x = 2;" no mesmo bloco
 *
 *   3. Tipo incompatível em atribuição
 *      → "int x = true;" (int ≠ bool)
 *
 *   4. Operandos incompatíveis em expressão binária
 *      → "1 + true" ou "\"texto\" - 2"
 *
 *   5. Operador aplicado a tipo errado
 *      → "!42" (negação lógica em int) ou "-true" (negação aritmética em bool)
 *
 *   6. Condição de if/while não é booleana
 *      → "if (42) { ... }"
 *
 *   7. Variável usada em read não foi declarada
 *      → "read(x)" sem "int x" antes
 *
 * Teoria aplicada: Gramáticas com Atributos — cada nó da AST "herda"
 * ou "sintetiza" informações de tipo que fluem pela árvore.
 */
public class SemanticAnalyzer implements NodeVisitor<DataType> {

    /** Tabela de símbolos com controle de escopo aninhado. */
    private final SymbolTable symbolTable = new SymbolTable();

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Ponto de entrada: analisa o programa completo.
     * Lança SemanticException ao primeiro erro encontrado.
     *
     * @param program raiz da AST gerada pelo Parser
     * @throws SemanticException se algum erro semântico for detectado
     */
    public void analyze(ProgramNode program) {
        program.accept(this);
    }

    // -------------------------------------------------------------------------
    // Visitors de Statement — todos retornam VOID
    // -------------------------------------------------------------------------

    @Override
    public DataType visitProgram(ProgramNode node) {
        // O programa inteiro vive num escopo global
        symbolTable.pushScope();
        for (var stmt : node.getStatements()) {
            stmt.accept(this);
        }
        symbolTable.popScope();
        return DataType.VOID;
    }

    @Override
    public DataType visitBlock(BlockStmt node) {
        // Cada bloco { } cria um novo escopo
        symbolTable.pushScope();
        for (var stmt : node.getStatements()) {
            stmt.accept(this);
        }
        symbolTable.popScope();
        return DataType.VOID;
    }

    @Override
    public DataType visitVarDecl(VarDeclStmt node) {
        String   name     = node.getNameToken().getLexeme();
        DataType declType = DataType.fromLexeme(node.getTypeToken().getLexeme());
        int      line     = node.getNameToken().getLine();

        // Se há inicializador, verifica se o tipo bate com o declarado
        if (node.hasInitializer()) {
            DataType initType = node.getInitializer().accept(this);

            if (initType != declType) {
                throw new SemanticException(String.format(
                    "Erro semântico [linha %d]: tipo incompatível na declaração de '%s' — " +
                    "declarado como '%s' mas inicializado com '%s'",
                    line, name, declType, initType
                ));
            }
        }

        // Registra a variável na tabela de símbolos do escopo atual
        symbolTable.declare(new Symbol(name, declType, line));
        return DataType.VOID;
    }

    @Override
    public DataType visitAssign(AssignStmt node) {
        String name = node.getNameToken().getLexeme();
        int    line = node.getNameToken().getLine();

        // A variável precisa ter sido declarada antes
        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: variável '%s' não foi declarada",
                line, name
            ));
        }

        // O tipo da expressão deve ser compatível com o tipo da variável
        DataType valueType = node.getValue().accept(this);
        if (valueType != symbol.getType()) {
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: não é possível atribuir '%s' à variável '%s' do tipo '%s'",
                line, valueType, name, symbol.getType()
            ));
        }

        return DataType.VOID;
    }

    @Override
    public DataType visitIf(IfStmt node) {
        // A condição DEVE ser do tipo bool
        DataType condType = node.getCondition().accept(this);
        if (condType != DataType.BOOL) {
            Token condToken = getExprToken(node.getCondition());
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: condição do 'if' deve ser do tipo 'bool', encontrado '%s'",
                condToken.getLine(), condType
            ));
        }

        node.getThenBranch().accept(this);
        if (node.hasElse()) {
            node.getElseBranch().accept(this);
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitWhile(WhileStmt node) {
        // A condição DEVE ser do tipo bool
        DataType condType = node.getCondition().accept(this);
        if (condType != DataType.BOOL) {
            Token condToken = getExprToken(node.getCondition());
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: condição do 'while' deve ser do tipo 'bool', encontrado '%s'",
                condToken.getLine(), condType
            ));
        }

        node.getBody().accept(this);
        return DataType.VOID;
    }

    @Override
    public DataType visitPrint(PrintStmt node) {
        // print aceita qualquer tipo — só precisa que a expressão seja válida
        node.getExpression().accept(this);
        return DataType.VOID;
    }

    @Override
    public DataType visitRead(ReadStmt node) {
        String name = node.getNameToken().getLexeme();
        int    line = node.getNameToken().getLine();

        // A variável precisa existir para receber o valor lido
        if (!symbolTable.isDeclared(name)) {
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: variável '%s' não foi declarada",
                line, name
            ));
        }
        return DataType.VOID;
    }

    // -------------------------------------------------------------------------
    // Visitors de Expressão — retornam o tipo inferido
    // -------------------------------------------------------------------------

    @Override
    public DataType visitBinary(BinaryExpr node) {
        DataType leftType  = node.getLeft().accept(this);
        DataType rightType = node.getRight().accept(this);
        Token    op        = node.getOperator();
        int      line      = op.getLine();

        return switch (op.getType()) {

            // Operadores aritméticos: ambos os lados devem ser INT → resultado INT
            case OP_PLUS, OP_MINUS, OP_MULTIPLY, OP_DIVIDE -> {
                requireType(leftType,  DataType.INT, "esquerdo", op.getLexeme(), line);
                requireType(rightType, DataType.INT, "direito",  op.getLexeme(), line);
                yield DataType.INT;
            }

            // Operadores relacionais numéricos: INT op INT → BOOL
            case OP_LESS_THAN, OP_GREATER_THAN,
                 OP_LESS_EQUAL, OP_GREATER_EQUAL -> {
                requireType(leftType,  DataType.INT, "esquerdo", op.getLexeme(), line);
                requireType(rightType, DataType.INT, "direito",  op.getLexeme(), line);
                yield DataType.BOOL;
            }

            // Igualdade: os dois lados precisam ter o MESMO tipo → BOOL
            case OP_EQUAL, OP_NOT_EQUAL -> {
                if (leftType != rightType) {
                    throw new SemanticException(String.format(
                        "Erro semântico [linha %d]: operador '%s' exige operandos do mesmo tipo, " +
                        "encontrado '%s' e '%s'",
                        line, op.getLexeme(), leftType, rightType
                    ));
                }
                yield DataType.BOOL;
            }

            // Operadores lógicos: ambos BOOL → BOOL
            case OP_AND, OP_OR -> {
                requireType(leftType,  DataType.BOOL, "esquerdo", op.getLexeme(), line);
                requireType(rightType, DataType.BOOL, "direito",  op.getLexeme(), line);
                yield DataType.BOOL;
            }

            // Concatenação de strings com +
            default -> throw new SemanticException(String.format(
                "Erro semântico [linha %d]: operador '%s' não suportado",
                line, op.getLexeme()
            ));
        };
    }

    @Override
    public DataType visitUnary(UnaryExpr node) {
        DataType operandType = node.getOperand().accept(this);
        Token    op          = node.getOperator();
        int      line        = op.getLine();

        return switch (op.getType()) {
            // Negação lógica: !bool → bool
            case OP_NOT -> {
                requireType(operandType, DataType.BOOL, "operando", "!", line);
                yield DataType.BOOL;
            }
            // Negação aritmética: -int → int
            case OP_MINUS -> {
                requireType(operandType, DataType.INT, "operando", "-", line);
                yield DataType.INT;
            }
            default -> throw new SemanticException(String.format(
                "Erro semântico [linha %d]: operador unário '%s' não reconhecido",
                line, op.getLexeme()
            ));
        };
    }

    @Override
    public DataType visitIdentifier(IdentifierExpr node) {
        String name = node.getName();
        int    line = node.getToken().getLine();

        // Busca a variável na tabela de símbolos
        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: variável '%s' não foi declarada",
                line, name
            ));
        }

        // Retorna o tipo registrado na tabela de símbolos
        return symbol.getType();
    }

    @Override
    public DataType visitIntLiteral(IntLiteralExpr node) {
        return DataType.INT;
    }

    @Override
    public DataType visitBoolLiteral(BoolLiteralExpr node) {
        return DataType.BOOL;
    }

    @Override
    public DataType visitStringLiteral(StringLiteralExpr node) {
        return DataType.STRING;
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    /**
     * Verifica se um tipo é o esperado; lança SemanticException se não for.
     * Centraliza a mensagem de erro de tipo para evitar repetição.
     *
     * @param actual   tipo que foi encontrado
     * @param expected tipo que era esperado
     * @param side     "esquerdo", "direito" ou "operando" (para a mensagem)
     * @param op       lexema do operador (para a mensagem)
     * @param line     linha do operador
     */
    private void requireType(DataType actual, DataType expected,
                             String side, String op, int line) {
        if (actual != expected) {
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: operador '%s' exige '%s' no lado %s, encontrado '%s'",
                line, op, expected, side, actual
            ));
        }
    }

    /**
     * Obtém o token principal de uma expressão qualquer.
     * Usado para reportar a linha correta nos erros de condição.
     */
    private Token getExprToken(compiler.ast.expr.Expr expr) {
        return expr.getToken();
    }
}
