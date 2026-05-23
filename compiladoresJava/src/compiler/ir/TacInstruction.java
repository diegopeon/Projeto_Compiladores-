package compiler.ir;

/**
 * Representa uma instrução de Código de Três Endereços (TAC).
 *
 * Cada instrução tem a forma:
 *   result = operand1  op  operand2
 *
 * Campos não utilizados por uma instrução específica ficam como null.
 *
 * Exemplos de instruções geradas:
 *
 *   Atribuição simples:   x = 42
 *   Operação binária:     t0 = x + y
 *   Operação unária:      t1 = -x
 *   Desvio incondicional: goto L0
 *   Desvio condicional:   if t0 goto L1
 *   Desvio negado:        ifFalse t0 goto L2
 *   Label:                L0:
 *   Print:                print x
 *   Read:                 read x
 *   Cópia:                x = t0
 *
 * Teoria aplicada: o TAC é a base para Esquemas de Tradução Dirigida
 * pela Sintaxe (SDT) — cada nó da AST emite suas instruções TAC.
 */
public class TacInstruction {

    /** Tipo da instrução — determina quais campos são relevantes. */
    public enum Op {
        // Operações aritméticas
        ADD, SUB, MUL, DIV,

        // Operações relacionais (resultado é bool)
        EQ, NEQ, LT, GT, LEQ, GEQ,

        // Operações lógicas
        AND, OR, NOT,

        // Negação aritmética unária
        NEG,

        // Atribuição / cópia simples: result = operand1
        COPY,

        // Desvio incondicional: goto label
        GOTO,

        // Desvio condicional:   if operand1 goto label
        IF_TRUE,

        // Desvio negado:        ifFalse operand1 goto label
        IF_FALSE,

        // Marcador de posição no código
        LABEL,

        // Entrada e saída
        PRINT,
        READ
    }

    /** Tipo desta instrução. */
    private final Op     op;

    /** Destino do resultado (lado esquerdo). Null para GOTO, LABEL, PRINT. */
    private final String result;

    /** Primeiro operando. Null para GOTO e LABEL. */
    private final String operand1;

    /** Segundo operando. Null para instruções unárias e de controle. */
    private final String operand2;

    /**
     * Construtor completo — usado internamente pela fábrica estática.
     */
    private TacInstruction(Op op, String result, String operand1, String operand2) {
        this.op       = op;
        this.result   = result;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    // -------------------------------------------------------------------------
    // Métodos fábrica — cada um cria um tipo específico de instrução
    // -------------------------------------------------------------------------

    /** result = op1  binaryOp  op2 */
    public static TacInstruction binary(Op op, String result,
                                        String op1, String op2) {
        return new TacInstruction(op, result, op1, op2);
    }

    /** result = unaryOp  operand */
    public static TacInstruction unary(Op op, String result, String operand) {
        return new TacInstruction(op, result, operand, null);
    }

    /** result = operand  (cópia simples) */
    public static TacInstruction copy(String result, String operand) {
        return new TacInstruction(Op.COPY, result, operand, null);
    }

    /** goto label */
    public static TacInstruction jump(String label) {
        return new TacInstruction(Op.GOTO, null, label, null);
    }

    /** if condition goto label */
    public static TacInstruction ifTrue(String condition, String label) {
        return new TacInstruction(Op.IF_TRUE, null, condition, label);
    }

    /** ifFalse condition goto label */
    public static TacInstruction ifFalse(String condition, String label) {
        return new TacInstruction(Op.IF_FALSE, null, condition, label);
    }

    /** label: */
    public static TacInstruction label(String name) {
        return new TacInstruction(Op.LABEL, null, name, null);
    }

    /** print operand */
    public static TacInstruction print(String operand) {
        return new TacInstruction(Op.PRINT, null, operand, null);
    }

    /** read result */
    public static TacInstruction read(String result) {
        return new TacInstruction(Op.READ, result, null, null);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Op     getOp()       { return op;       }
    public String getResult()   { return result;   }
    public String getOperand1() { return operand1; }
    public String getOperand2() { return operand2; }

    // -------------------------------------------------------------------------
    // Representação textual — formato legível para debug e relatório
    // -------------------------------------------------------------------------

    /**
     * Formata a instrução em texto legível.
     *
     * Exemplos:
     *   t0 = x + y
     *   t1 = !t0
     *   ifFalse t1 goto L2
     *   L0:
     *   print soma
     */
    @Override
    public String toString() {
        return switch (op) {
            case ADD  -> result + " = " + operand1 + " + "  + operand2;
            case SUB  -> result + " = " + operand1 + " - "  + operand2;
            case MUL  -> result + " = " + operand1 + " * "  + operand2;
            case DIV  -> result + " = " + operand1 + " / "  + operand2;
            case EQ   -> result + " = " + operand1 + " == " + operand2;
            case NEQ  -> result + " = " + operand1 + " != " + operand2;
            case LT   -> result + " = " + operand1 + " < "  + operand2;
            case GT   -> result + " = " + operand1 + " > "  + operand2;
            case LEQ  -> result + " = " + operand1 + " <= " + operand2;
            case GEQ  -> result + " = " + operand1 + " >= " + operand2;
            case AND  -> result + " = " + operand1 + " && " + operand2;
            case OR   -> result + " = " + operand1 + " || " + operand2;
            case NOT  -> result + " = !" + operand1;
            case NEG  -> result + " = -" + operand1;
            case COPY -> result + " = "  + operand1;
            case GOTO     -> "goto "    + operand1;
            case IF_TRUE  -> "if "      + operand1 + " goto " + operand2;
            case IF_FALSE -> "ifFalse " + operand1 + " goto " + operand2;
            case LABEL    -> operand1   + ":";
            case PRINT    -> "print "   + operand1;
            case READ     -> "read "    + result;
        };
    }
}
