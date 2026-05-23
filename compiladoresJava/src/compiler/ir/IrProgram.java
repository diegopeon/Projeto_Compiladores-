package compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contém o programa completo em Código de Três Endereços (TAC).
 *
 * É o resultado final da Etapa 4 — uma lista ordenada de instruções
 * TAC que representa o programa de forma linear e independente de máquina.
 *
 * Esta representação é a entrada da Etapa 5 (geração de código final).
 */
public class IrProgram {

    /** Lista de instruções TAC na ordem de execução. */
    private final List<TacInstruction> instructions = new ArrayList<>();

    /**
     * Adiciona uma instrução ao final do programa.
     *
     * @param instruction instrução a adicionar
     */
    public void emit(TacInstruction instruction) {
        instructions.add(instruction);
    }

    /**
     * Retorna a lista de instruções como visão somente-leitura.
     *
     * @return lista imutável de instruções TAC
     */
    public List<TacInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Imprime o programa TAC formatado — útil para debug e relatório.
     *
     * Labels são impressos sem indentação;
     * demais instruções são indentadas com 4 espaços.
     */
    public void print() {
        System.out.println("=== Código Intermediário (TAC) ===");
        for (TacInstruction instr : instructions) {
            boolean isLabel = instr.getOp() == TacInstruction.Op.LABEL;
            System.out.println(isLabel ? instr : "    " + instr);
        }
        System.out.println("=== Fim do TAC ===");
    }

    /**
     * Retorna a contagem total de instruções geradas.
     * Útil para métricas e relatório.
     */
    public int size() {
        return instructions.size();
    }
}
