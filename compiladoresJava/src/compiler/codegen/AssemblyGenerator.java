package compiler.codegen;

import compiler.ir.IrProgram;
import compiler.ir.TacInstruction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gerador de Código Assembly (x86-64 AT&T syntax) — Etapa 5 do compilador.
 *
 * Traduz cada instrução TAC para sua equivalente em Assembly x86-64.
 * O código gerado usa a convenção de chamada System V AMD64 ABI (Linux).
 *
 * Estratégia de alocação de registradores:
 *   - Variáveis e temporários são mapeados para posições na pilha (%rbp - offset)
 *   - Operações usam %rax e %rbx como registradores de trabalho
 *   - print usa a syscall write (Linux) via %rdi, %rsi, %rdx
 *
 * Nota didática: este é um gerador simplificado focado em clareza.
 * Um compilador de produção faria alocação de registradores por
 * coloração de grafo de interferência para maximizar o uso de regs.
 */
public class AssemblyGenerator {

    /** Código Assembly sendo construído. */
    private final StringBuilder asm = new StringBuilder();

    /** Nomes de todas as variáveis/temporários encontrados (para alocar na pilha). */
    private final Set<String> variables = new HashSet<>();

    /** Offset atual na pilha para alocação de variáveis locais. */
    private int stackOffset = 0;

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Gera o código Assembly completo a partir do programa TAC.
     *
     * @param program programa TAC gerado pela Etapa 4
     * @return string com o código Assembly completo
     */
    public String generate(IrProgram program) {
        List<TacInstruction> instructions = program.getInstructions();

        // Primeira passagem: coleta todos os nomes de variáveis e temporários
        collectVariables(instructions);

        // Seção de dados (strings usadas em print)
        emitDataSection(instructions);

        // Seção de texto (código executável)
        emitTextSection(instructions);

        return asm.toString();
    }

    // -------------------------------------------------------------------------
    // Coleta de variáveis
    // -------------------------------------------------------------------------

    /**
     * Percorre as instruções e coleta todos os identificadores únicos
     * para reservar espaço na pilha.
     */
    private void collectVariables(List<TacInstruction> instructions) {
        for (TacInstruction instr : instructions) {
            addVar(instr.getResult());
            addVar(instr.getOperand1());
            addVar(instr.getOperand2());
        }
    }

    /**
     * Adiciona um nome à tabela de variáveis se for um identificador válido
     * (não nulo, não numérico, não literal booleano, não label, não string).
     */
    private void addVar(String name) {
        if (name == null) return;
        if (name.startsWith("\"")) return;          // string literal
        if (name.startsWith("L")) return;            // label
        if (name.equals("true") || name.equals("false")) return;
        if (name.matches("-?\\d+")) return;          // número
        if (!variables.contains(name)) {
            variables.add(name);
            stackOffset += 8; // cada variável ocupa 8 bytes (64-bit)
        }
    }

    // -------------------------------------------------------------------------
    // Seção de dados
    // -------------------------------------------------------------------------

    private void emitDataSection(List<TacInstruction> instructions) {
        emit(".section .data");

        // Formato de print para inteiros
        emit("fmt_int:   .string \"%ld\\n\"");
        emit("fmt_bool_t: .string \"true\\n\"");
        emit("fmt_bool_f: .string \"false\\n\"");

        // Strings literais encontradas nas instruções
        int strIdx = 0;
        for (TacInstruction instr : instructions) {
            if (instr.getOp() == TacInstruction.Op.PRINT) {
                String op = instr.getOperand1();
                if (op != null && op.startsWith("\"")) {
                    String content = op.substring(1, op.length() - 1);
                    emit("str_" + strIdx + ": .string \"" + content + "\\n\"");
                    strIdx++;
                }
            }
        }
        emit("");
    }

    // -------------------------------------------------------------------------
    // Seção de texto
    // -------------------------------------------------------------------------

    private void emitTextSection(List<TacInstruction> instructions) {
        emit(".section .text");
        emit(".global main");
        emit("");
        emit("main:");

        // Prólogo: configura o frame da pilha
        emit("    pushq %rbp");
        emit("    movq  %rsp, %rbp");
        // Reserva espaço para todas as variáveis locais (alinhado a 16 bytes)
        int frameSize = ((stackOffset + 15) / 16) * 16;
        if (frameSize > 0) {
            emit("    subq  $" + frameSize + ", %rsp");
        }
        emit("");

        // Gera código para cada instrução TAC
        int strIdx = 0;
        for (TacInstruction instr : instructions) {
            emitInstruction(instr, strIdx);
            if (instr.getOp() == TacInstruction.Op.PRINT &&
                instr.getOperand1() != null &&
                instr.getOperand1().startsWith("\"")) {
                strIdx++;
            }
        }

        // Epílogo: restaura a pilha e retorna 0
        emit("");
        emit("    # --- epílogo ---");
        emit("    movq  $0, %rax");
        emit("    movq  %rbp, %rsp");
        emit("    popq  %rbp");
        emit("    ret");
    }

    // -------------------------------------------------------------------------
    // Geração de instrução individual
    // -------------------------------------------------------------------------

    private void emitInstruction(TacInstruction instr, int strIdx) {
        switch (instr.getOp()) {

            case COPY -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                storeFromRax(instr.getResult());
            }

            case ADD, SUB, MUL, DIV -> emitArithmetic(instr);

            case EQ, NEQ, LT, GT, LEQ, GEQ -> emitComparison(instr);

            case AND -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                emit("    movq  %rax, %rbx");
                loadToRax(instr.getOperand2());
                emit("    andq  %rbx, %rax");
                storeFromRax(instr.getResult());
            }

            case OR -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                emit("    movq  %rax, %rbx");
                loadToRax(instr.getOperand2());
                emit("    orq   %rbx, %rax");
                storeFromRax(instr.getResult());
            }

            case NOT -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                emit("    xorq  $1, %rax");
                storeFromRax(instr.getResult());
            }

            case NEG -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                emit("    negq  %rax");
                storeFromRax(instr.getResult());
            }

            case LABEL -> emit("\n" + instr.getOperand1() + ":");

            case GOTO -> {
                emit("    jmp   " + instr.getOperand1());
            }

            case IF_TRUE -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                emit("    cmpq  $0, %rax");
                emit("    jne   " + instr.getOperand2());
            }

            case IF_FALSE -> {
                emit("");
                emit("    # " + instr);
                loadToRax(instr.getOperand1());
                emit("    cmpq  $0, %rax");
                emit("    je    " + instr.getOperand2());
            }

            case PRINT -> emitPrint(instr, strIdx);

            case READ -> {
                // Simplificado: lê um inteiro via scanf
                emit("");
                emit("    # " + instr);
                emit("    leaq  " + getOffset(instr.getResult()) + ", %rsi");
                emit("    leaq  fmt_int(%rip), %rdi");
                emit("    xorq  %rax, %rax");
                emit("    call  scanf");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de geração
    // -------------------------------------------------------------------------

    private void emitArithmetic(TacInstruction instr) {
        emit("");
        emit("    # " + instr);
        loadToRax(instr.getOperand1());
        emit("    movq  %rax, %rbx");
        loadToRax(instr.getOperand2());

        switch (instr.getOp()) {
            case ADD -> { emit("    addq  %rbx, %rax"); }
            case SUB -> { emit("    subq  %rax, %rbx"); emit("    movq  %rbx, %rax"); }
            case MUL -> { emit("    imulq %rbx, %rax"); }
            case DIV -> {
                emit("    movq  %rbx, %rcx");
                emit("    movq  %rcx, %rax");
                emit("    cqto");
                emit("    idivq " + resolveOperand(instr.getOperand2()));
            }
            default -> {}
        }
        storeFromRax(instr.getResult());
    }

    private void emitComparison(TacInstruction instr) {
        emit("");
        emit("    # " + instr);
        loadToRax(instr.getOperand1());
        emit("    movq  %rax, %rbx");
        loadToRax(instr.getOperand2());
        emit("    cmpq  %rax, %rbx");

        String setInstr = switch (instr.getOp()) {
            case EQ  -> "sete";
            case NEQ -> "setne";
            case LT  -> "setl";
            case GT  -> "setg";
            case LEQ -> "setle";
            case GEQ -> "setge";
            default  -> "sete";
        };

        emit("    " + setInstr + "  %al");
        emit("    movzbq %al, %rax");
        storeFromRax(instr.getResult());
    }

    private void emitPrint(TacInstruction instr, int strIdx) {
        emit("");
        emit("    # " + instr);
        String op = instr.getOperand1();

        if (op.startsWith("\"")) {
            // String literal → usa puts via lea
            emit("    leaq  str_" + strIdx + "(%rip), %rdi");
            emit("    call  printf");
        } else {
            // Variável ou temporário → imprime como inteiro
            loadToRax(op);
            emit("    movq  %rax, %rsi");
            emit("    leaq  fmt_int(%rip), %rdi");
            emit("    xorq  %rax, %rax");
            emit("    call  printf");
        }
    }

    /**
     * Carrega um valor (variável, literal ou bool) em %rax.
     */
    private void loadToRax(String operand) {
        if (operand == null) return;

        if (operand.matches("-?\\d+")) {
            emit("    movq  $" + operand + ", %rax");
        } else if (operand.equals("true")) {
            emit("    movq  $1, %rax");
        } else if (operand.equals("false")) {
            emit("    movq  $0, %rax");
        } else {
            emit("    movq  " + getOffset(operand) + ", %rax");
        }
    }

    /**
     * Armazena %rax na posição de memória da variável/temporário.
     */
    private void storeFromRax(String name) {
        if (name == null) return;
        emit("    movq  %rax, " + getOffset(name));
    }

    /**
     * Retorna o offset na pilha para uma variável.
     * Aloca novos slots conforme necessário.
     */
    private int varOffset = 0;
    private final java.util.Map<String, Integer> offsets = new java.util.LinkedHashMap<>();

    private String getOffset(String name) {
        if (!offsets.containsKey(name)) {
            varOffset += 8;
            offsets.put(name, varOffset);
        }
        return "-" + offsets.get(name) + "(%rbp)";
    }

    private String resolveOperand(String operand) {
        if (operand == null) return "%rax";
        if (operand.matches("-?\\d+")) return "$" + operand;
        return getOffset(operand);
    }

    private void emit(String line) {
        asm.append(line).append("\n");
    }
}
