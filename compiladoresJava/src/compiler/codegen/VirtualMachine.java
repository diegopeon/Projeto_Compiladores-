package compiler.codegen;

import compiler.ir.IrProgram;
import compiler.ir.TacInstruction;

import java.util.*;

/**
 * Máquina Virtual (VM) que executa o Código de Três Endereços (TAC).
 *
 * Em vez de rodar Assembly em hardware real, esta VM interpreta as
 * instruções TAC diretamente em Java — permitindo executar e ver o
 * resultado do programa compilado sem precisar de um linker externo.
 *
 * Funciona como um computador simplificado:
 *   - Memória: mapa de nome → valor (substitui registradores e pilha)
 *   - Program Counter (PC): índice da instrução atual
 *   - Jump: modifica o PC para implementar desvios e laços
 *
 * Esta abordagem é usada por linguagens como Java (JVM) e Python
 * para executar bytecode de forma portável e segura.
 */
public class VirtualMachine {

    // -------------------------------------------------------------------------
    // Estado da VM
    // -------------------------------------------------------------------------

    /**
     * Memória da VM: mapeia nomes de variáveis/temporários para seus valores.
     * Valores são armazenados como Object para suportar int, boolean e String.
     */
    private final Map<String, Object> memory = new HashMap<>();

    /**
     * Mapa de labels para índices de instrução.
     * Construído na inicialização para permitir saltos O(1).
     */
    private final Map<String, Integer> labelIndex = new HashMap<>();

    /** Saída produzida pelo programa (acumulada pelo print). */
    private final List<String> output = new ArrayList<>();

    /** Número máximo de instruções antes de abortar (proteção contra loop infinito). */
    private static final int MAX_STEPS = 100_000;

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Executa o programa TAC e retorna as linhas de saída produzidas.
     *
     * @param program  programa TAC gerado pela Etapa 4
     * @param inputs   valores de entrada para instruções read (em ordem)
     * @return lista de strings produzidas por print
     */
    public List<String> execute(IrProgram program, List<Object> inputs) {
        memory.clear();
        labelIndex.clear();
        output.clear();

        List<TacInstruction> instructions = program.getInstructions();
        buildLabelIndex(instructions);

        Queue<Object> inputQueue = new LinkedList<>(inputs);
        int pc    = 0;
        int steps = 0;

        while (pc < instructions.size()) {
            if (++steps > MAX_STEPS) {
                output.add("[VM] Limite de execução atingido — possível loop infinito.");
                break;
            }

            TacInstruction instr = instructions.get(pc);
            int nextPc = pc + 1; // avanço padrão

            switch (instr.getOp()) {

                case COPY -> {
                    Object val = resolve(instr.getOperand1());
                    memory.put(instr.getResult(), val);
                }

                case ADD -> {
                    int l = toInt(resolve(instr.getOperand1()));
                    int r = toInt(resolve(instr.getOperand2()));
                    memory.put(instr.getResult(), l + r);
                }

                case SUB -> {
                    int l = toInt(resolve(instr.getOperand1()));
                    int r = toInt(resolve(instr.getOperand2()));
                    memory.put(instr.getResult(), l - r);
                }

                case MUL -> {
                    int l = toInt(resolve(instr.getOperand1()));
                    int r = toInt(resolve(instr.getOperand2()));
                    memory.put(instr.getResult(), l * r);
                }

                case DIV -> {
                    int l = toInt(resolve(instr.getOperand1()));
                    int r = toInt(resolve(instr.getOperand2()));
                    if (r == 0) {
                        output.add("[VM] Erro: divisão por zero.");
                        return output;
                    }
                    memory.put(instr.getResult(), l / r);
                }

                case EQ  -> {
                    Object l = resolve(instr.getOperand1());
                    Object r = resolve(instr.getOperand2());
                    memory.put(instr.getResult(), l.equals(r));
                }

                case NEQ -> {
                    Object l = resolve(instr.getOperand1());
                    Object r = resolve(instr.getOperand2());
                    memory.put(instr.getResult(), !l.equals(r));
                }

                case LT  -> {
                    memory.put(instr.getResult(),
                        toInt(resolve(instr.getOperand1())) <
                        toInt(resolve(instr.getOperand2())));
                }

                case GT  -> {
                    memory.put(instr.getResult(),
                        toInt(resolve(instr.getOperand1())) >
                        toInt(resolve(instr.getOperand2())));
                }

                case LEQ -> {
                    memory.put(instr.getResult(),
                        toInt(resolve(instr.getOperand1())) <=
                        toInt(resolve(instr.getOperand2())));
                }

                case GEQ -> {
                    memory.put(instr.getResult(),
                        toInt(resolve(instr.getOperand1())) >=
                        toInt(resolve(instr.getOperand2())));
                }

                case AND -> {
                    boolean l = toBool(resolve(instr.getOperand1()));
                    boolean r = toBool(resolve(instr.getOperand2()));
                    memory.put(instr.getResult(), l && r);
                }

                case OR -> {
                    boolean l = toBool(resolve(instr.getOperand1()));
                    boolean r = toBool(resolve(instr.getOperand2()));
                    memory.put(instr.getResult(), l || r);
                }

                case NOT -> {
                    boolean v = toBool(resolve(instr.getOperand1()));
                    memory.put(instr.getResult(), !v);
                }

                case NEG -> {
                    int v = toInt(resolve(instr.getOperand1()));
                    memory.put(instr.getResult(), -v);
                }

                case GOTO -> {
                    nextPc = labelIndex.get(instr.getOperand1());
                }

                case IF_TRUE -> {
                    if (toBool(resolve(instr.getOperand1()))) {
                        nextPc = labelIndex.get(instr.getOperand2());
                    }
                }

                case IF_FALSE -> {
                    if (!toBool(resolve(instr.getOperand1()))) {
                        nextPc = labelIndex.get(instr.getOperand2());
                    }
                }

                case LABEL -> { /* apenas marcador, sem ação */ }

                case PRINT -> {
                    Object val = resolve(instr.getOperand1());
                    output.add(formatOutput(val));
                }

                case READ -> {
                    Object input = inputQueue.isEmpty() ? 0 : inputQueue.poll();
                    memory.put(instr.getResult(), input);
                }
            }

            pc = nextPc;
        }

        return output;
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    /**
     * Constrói o mapa de labels para índices de instrução na primeira passagem.
     * Permite saltos em O(1) durante a execução.
     */
    private void buildLabelIndex(List<TacInstruction> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            TacInstruction instr = instructions.get(i);
            if (instr.getOp() == TacInstruction.Op.LABEL) {
                labelIndex.put(instr.getOperand1(), i + 1);
            }
        }
    }

    /**
     * Resolve o valor de um operando:
     *   - Literal inteiro  → Integer
     *   - Literal booleano → Boolean
     *   - Literal string   → String (sem aspas)
     *   - Variável         → valor na memória (0 se não inicializada)
     */
    private Object resolve(String operand) {
        if (operand == null) return 0;

        // Literal inteiro
        if (operand.matches("-?\\d+")) return Integer.parseInt(operand);

        // Literais booleanos
        if (operand.equals("true"))  return true;
        if (operand.equals("false")) return false;

        // Literal string (remove as aspas)
        if (operand.startsWith("\"") && operand.endsWith("\"")) {
            return operand.substring(1, operand.length() - 1);
        }

        // Variável — retorna 0 se nunca foi inicializada
        return memory.getOrDefault(operand, 0);
    }

    /** Converte um valor para int. Boolean vira 1 (true) ou 0 (false). */
    private int toInt(Object val) {
        if (val instanceof Integer i) return i;
        if (val instanceof Boolean b) return b ? 1 : 0;
        return 0;
    }

    /** Converte um valor para boolean. Integer 0 é false, qualquer outro é true. */
    private boolean toBool(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof Integer i) return i != 0;
        return false;
    }

    /** Formata um valor para exibição. */
    private String formatOutput(Object val) {
        if (val == null) return "null";
        return val.toString();
    }
}
