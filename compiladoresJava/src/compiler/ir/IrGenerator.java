package compiler.ir;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.*;
import compiler.ast.stmt.*;
import compiler.lexer.TokenType;

/**
 * Gerador de Código Intermediário (IR) — Etapa 4 do compilador.
 *
 * Implementa NodeVisitor<String>: percorre a AST e para cada nó
 * emite as instruções TAC correspondentes em {@link IrProgram}.
 * Para expressões, retorna o nome do temporário ou variável que
 * contém o resultado — esse valor é usado pelo nó pai.
 *
 * Estratégias de geração:
 *
 *   Expressões binárias:
 *     left  → gera código para o lado esquerdo, retorna t_left
 *     right → gera código para o lado direito, retorna t_right
 *     emite: t_new = t_left OP t_right
 *     retorna: t_new
 *
 *   If-else:
 *     cond  → gera código da condição, retorna t_cond
 *     emite: ifFalse t_cond goto L_else
 *     then  → gera código do bloco then
 *     emite: goto L_end
 *     emite: L_else:
 *     else  → gera código do bloco else (se existir)
 *     emite: L_end:
 *
 *   While:
 *     emite: L_start:
 *     cond  → gera código da condição
 *     emite: ifFalse t_cond goto L_end
 *     body  → gera código do corpo
 *     emite: goto L_start
 *     emite: L_end:
 *
 * Teoria aplicada: Esquemas de Tradução Dirigida pela Sintaxe (SDT)
 * com atributos sintetizados — o "lugar" do resultado sobe pela árvore.
 */
public class IrGenerator implements NodeVisitor<String> {

    /** Programa TAC sendo construído — acumulamos as instruções aqui. */
    private final IrProgram program = new IrProgram();

    /** Contador global de temporários: t0, t1, t2, ... */
    private int tempCount  = 0;

    /** Contador global de labels: L0, L1, L2, ... */
    private int labelCount = 0;

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Gera o código TAC para o programa inteiro.
     *
     * @param programNode raiz da AST (já validada pelo SemanticAnalyzer)
     * @return o programa TAC gerado
     */
    public IrProgram generate(ProgramNode programNode) {
        programNode.accept(this);
        return program;
    }

    // -------------------------------------------------------------------------
    // Helpers de geração
    // -------------------------------------------------------------------------

    /**
     * Cria e retorna o nome de um novo temporário único: t0, t1, t2, ...
     */
    private String newTemp() {
        return "t" + tempCount++;
    }

    /**
     * Cria e retorna o nome de um novo label único: L0, L1, L2, ...
     */
    private String newLabel() {
        return "L" + labelCount++;
    }

    /**
     * Emite uma instrução TAC no programa.
     * Método centralizador para manter o código dos visitors limpo.
     */
    private void emit(TacInstruction instruction) {
        program.emit(instruction);
    }

    // -------------------------------------------------------------------------
    // Visitors de Statement
    // -------------------------------------------------------------------------

    @Override
    public String visitProgram(ProgramNode node) {
        for (var stmt : node.getStatements()) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public String visitBlock(BlockStmt node) {
        for (var stmt : node.getStatements()) {
            stmt.accept(this);
        }
        return null;
    }

    /**
     * Declaração de variável: int x = expr;
     *
     * Se há inicializador, gera o código da expressão e copia para x.
     * Sem inicializador, não emite nada (variável começa indefinida).
     *
     *   int x = 2 + 3;
     *   →  t0 = 2 + 3
     *   →  x  = t0
     */
    @Override
    public String visitVarDecl(VarDeclStmt node) {
        if (node.hasInitializer()) {
            String src = node.getInitializer().accept(this);
            emit(TacInstruction.copy(node.getNameToken().getLexeme(), src));
        }
        return null;
    }

    /**
     * Atribuição: x = expr;
     *
     *   x = y + 1;
     *   →  t0 = y + 1
     *   →  x  = t0
     */
    @Override
    public String visitAssign(AssignStmt node) {
        String src = node.getValue().accept(this);
        emit(TacInstruction.copy(node.getNameToken().getLexeme(), src));
        return null;
    }

    /**
     * Condicional if-else:
     *
     *   if (cond) { then } else { else }
     *   →  t0        = [código da condição]
     *   →  ifFalse t0 goto L_else
     *   →  [código do then]
     *   →  goto L_end
     *   →  L_else:
     *   →  [código do else, se existir]
     *   →  L_end:
     */
    @Override
    public String visitIf(IfStmt node) {
        String lElse = newLabel();
        String lEnd  = newLabel();

        // Gera condição e desvia para else se falsa
        String cond = node.getCondition().accept(this);
        emit(TacInstruction.ifFalse(cond, lElse));

        // Bloco then
        node.getThenBranch().accept(this);
        emit(TacInstruction.jump(lEnd));

        // Label do else
        emit(TacInstruction.label(lElse));
        if (node.hasElse()) {
            node.getElseBranch().accept(this);
        }

        // Label de fim
        emit(TacInstruction.label(lEnd));
        return null;
    }

    /**
     * Laço while:
     *
     *   while (cond) { body }
     *   →  L_start:
     *   →  t0        = [código da condição]
     *   →  ifFalse t0 goto L_end
     *   →  [código do body]
     *   →  goto L_start
     *   →  L_end:
     */
    @Override
    public String visitWhile(WhileStmt node) {
        String lStart = newLabel();
        String lEnd   = newLabel();

        // Início do laço — ponto de re-avaliação da condição
        emit(TacInstruction.label(lStart));

        // Condição: sai do laço se falsa
        String cond = node.getCondition().accept(this);
        emit(TacInstruction.ifFalse(cond, lEnd));

        // Corpo do laço
        node.getBody().accept(this);

        // Volta para testar a condição
        emit(TacInstruction.jump(lStart));

        // Ponto de saída do laço
        emit(TacInstruction.label(lEnd));
        return null;
    }

    /**
     * print(expr);
     *
     *   print(x + 1);
     *   →  t0 = x + 1
     *   →  print t0
     */
    @Override
    public String visitPrint(PrintStmt node) {
        String operand = node.getExpression().accept(this);
        emit(TacInstruction.print(operand));
        return null;
    }

    /**
     * read(x);
     *
     *   →  read x
     */
    @Override
    public String visitRead(ReadStmt node) {
        emit(TacInstruction.read(node.getNameToken().getLexeme()));
        return null;
    }

    // -------------------------------------------------------------------------
    // Visitors de Expressão — retornam o nome do resultado (temp ou variável)
    // -------------------------------------------------------------------------

    /**
     * Expressão binária: left OP right
     *
     * Gera código para ambos os lados e emite a instrução da operação.
     * Retorna o temporário com o resultado.
     *
     *   2 + 3 * 4
     *   →  t0 = 3 * 4
     *   →  t1 = 2 + t0
     *   retorna: t1
     */
    @Override
    public String visitBinary(BinaryExpr node) {
        String left  = node.getLeft().accept(this);
        String right = node.getRight().accept(this);
        String temp  = newTemp();

        TacInstruction.Op op = mapBinaryOp(node.getOperator().getType());
        emit(TacInstruction.binary(op, temp, left, right));
        return temp;
    }

    /**
     * Expressão unária: OP operand
     *
     *   !ativo
     *   →  t0 = !ativo
     *   retorna: t0
     */
    @Override
    public String visitUnary(UnaryExpr node) {
        String operand = node.getOperand().accept(this);
        String temp    = newTemp();

        TacInstruction.Op op = node.getOperator().getType() == TokenType.OP_NOT
            ? TacInstruction.Op.NOT
            : TacInstruction.Op.NEG;

        emit(TacInstruction.unary(op, temp, operand));
        return temp;
    }

    /**
     * Referência a variável — retorna o nome dela diretamente.
     * Não emite instrução: o nome da variável já é um operando válido no TAC.
     */
    @Override
    public String visitIdentifier(IdentifierExpr node) {
        return node.getName();
    }

    /**
     * Literal inteiro — retorna o valor como string.
     * Ex: 42 → retorna "42" (usado diretamente como operando TAC).
     */
    @Override
    public String visitIntLiteral(IntLiteralExpr node) {
        return String.valueOf(node.getValue());
    }

    /**
     * Literal booleano — retorna "true" ou "false".
     */
    @Override
    public String visitBoolLiteral(BoolLiteralExpr node) {
        return String.valueOf(node.getValue());
    }

    /**
     * Literal de string — retorna o valor entre aspas.
     */
    @Override
    public String visitStringLiteral(StringLiteralExpr node) {
        return "\"" + node.getValue() + "\"";
    }

    // -------------------------------------------------------------------------
    // Helper de mapeamento de operadores
    // -------------------------------------------------------------------------

    /**
     * Converte um TokenType de operador binário para o Op do TAC.
     */
    private TacInstruction.Op mapBinaryOp(TokenType tokenType) {
        return switch (tokenType) {
            case OP_PLUS          -> TacInstruction.Op.ADD;
            case OP_MINUS         -> TacInstruction.Op.SUB;
            case OP_MULTIPLY      -> TacInstruction.Op.MUL;
            case OP_DIVIDE        -> TacInstruction.Op.DIV;
            case OP_EQUAL         -> TacInstruction.Op.EQ;
            case OP_NOT_EQUAL     -> TacInstruction.Op.NEQ;
            case OP_LESS_THAN     -> TacInstruction.Op.LT;
            case OP_GREATER_THAN  -> TacInstruction.Op.GT;
            case OP_LESS_EQUAL    -> TacInstruction.Op.LEQ;
            case OP_GREATER_EQUAL -> TacInstruction.Op.GEQ;
            case OP_AND           -> TacInstruction.Op.AND;
            case OP_OR            -> TacInstruction.Op.OR;
            default -> throw new IllegalArgumentException(
                "Operador binário não mapeado: " + tokenType
            );
        };
    }
}
