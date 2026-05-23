package compiler;

import compiler.ast.stmt.ProgramNode;
import compiler.codegen.AssemblyGenerator;
import compiler.codegen.VirtualMachine;
import compiler.ir.IrGenerator;
import compiler.ir.IrProgram;
import compiler.ir.TacInstruction;
import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.parser.AstPrinter;
import compiler.parser.ParseException;
import compiler.parser.Parser;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Ponto de entrada interativo do compilador.
 *
 * O usuario digita seu proprio codigo-fonte, pressiona Enter duas vezes,
 * e o pipeline completo e executado — exibindo tokens, AST, TAC,
 * Assembly e o resultado real da execucao na Maquina Virtual.
 *
 * Comandos especiais:
 *   :sair   -> encerra o compilador
 *   :ajuda  -> exibe a referencia da linguagem
 *   :limpar -> limpa a tela
 */
public class Main {

    private static final String SEP  = "-".repeat(60);
    private static final String SEP2 = "=".repeat(60);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        banner();

        while (true) {
            String sourceCode = lerCodigo();
            if (sourceCode == null) break;          // usuario digitou :sair
            if (sourceCode.isBlank()) continue;     // linha vazia, tenta de novo

            compilarEExecutar(sourceCode);

            System.out.println("\nPressione Enter para compilar outro programa...");
            scanner.nextLine();
        }

        System.out.println("\nAte mais! o/\n");
    }

    // -------------------------------------------------------------------------
    // Leitura interativa do codigo-fonte
    // -------------------------------------------------------------------------

    /**
     * Le o codigo digitado pelo usuario linha a linha.
     * Uma linha vazia encerra a entrada e dispara a compilacao.
     *
     * @return o codigo-fonte completo, ou null se o usuario quiser sair
     */
    private static String lerCodigo() {
        System.out.println("\n" + SEP2);
        System.out.println("  Digite seu programa abaixo.");
        System.out.println("  Linha vazia  -> compilar e executar");
        System.out.println("  :ajuda       -> referencia da linguagem");
        System.out.println("  :sair        -> encerrar o compilador");
        System.out.println(SEP2);
        System.out.println();

        StringBuilder sb = new StringBuilder();
        int lineNumber = 1;

        while (true) {
            System.out.printf("  %2d | ", lineNumber);
            String line = scanner.nextLine();

            // Comandos especiais
            if (line.trim().equals(":sair"))  return null;
            if (line.trim().equals(":ajuda")) { ajuda(); continue; }
            if (line.trim().equals(":limpar")){ limparTela(); lineNumber = 1; sb.setLength(0); continue; }

            // Linha vazia = fim da entrada
            if (line.isBlank() && sb.length() > 0) break;

            sb.append(line).append("\n");
            lineNumber++;
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Pipeline completo
    // -------------------------------------------------------------------------

    /**
     * Executa as 5 etapas do compilador sobre o codigo fornecido.
     */
    private static void compilarEExecutar(String sourceCode) {
        System.out.println("\n" + SEP2);
        System.out.println("  COMPILANDO...");
        System.out.println(SEP2);

        // ── [1] Lexico ────────────────────────────────────────────────────────
        titulo("[1] Analise Lexica");
        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        if (lexer.hasErrors()) {
            lexer.getErrors().forEach(e -> erro(e));
            encerrarComErro("Corriga os erros lexicos e tente novamente."); return;
        }
        System.out.println("  Tokens reconhecidos:\n");
        for (Token t : tokens) {
            if (t.isType(compiler.lexer.TokenType.EOF)) continue;
            System.out.printf("    %-22s  \"%s\"%n",
                t.getType(), t.getLexeme());
        }
        ok((tokens.size() - 1) + " tokens — sem erros lexicos");

        // ── [2] Sintatico ─────────────────────────────────────────────────────
        titulo("[2] Analise Sintatica");
        ProgramNode ast;
        try {
            ast = new Parser(tokens).parse();
        } catch (ParseException e) {
            erro(e.getMessage());
            encerrarComErro("Corriga os erros sintaticos e tente novamente."); return;
        }
        System.out.println("  Arvore de Sintaxe Abstrata (AST):\n");

        // Captura e imprime a AST indentada
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream old = System.out;
        System.setOut(new java.io.PrintStream(baos));
        new AstPrinter().print(ast);
        System.setOut(old);
        for (String l : baos.toString().split("\n")) {
            System.out.println("    " + l);
        }
        System.out.println();
        ok("AST valida — " + ast.getStatements().size() + " instrucoes de nivel superior");

        // ── [3] Semantico ─────────────────────────────────────────────────────
        titulo("[3] Analise Semantica");
        try {
            new SemanticAnalyzer().analyze(ast);
            ok("Tipos e escopos verificados — nenhum erro encontrado");
        } catch (SemanticException e) {
            erro(e.getMessage());
            encerrarComErro("Corriga os erros semanticos e tente novamente."); return;
        }

        // ── [4] TAC ───────────────────────────────────────────────────────────
        titulo("[4] Codigo Intermediario (TAC)");
        IrProgram ir = new IrGenerator().generate(ast);
        System.out.println("  Instrucoes geradas:\n");
        for (TacInstruction instr : ir.getInstructions()) {
            boolean isLabel = instr.getOp() == TacInstruction.Op.LABEL;
            System.out.println(isLabel ? "    " + instr : "        " + instr);
        }
        System.out.println();
        ok(ir.size() + " instrucoes TAC geradas");

        // ── [5a] Assembly ─────────────────────────────────────────────────────
        titulo("[5a] Assembly x86-64 gerado");
        System.out.println("  (codigo pronto para montar com gcc)\n");
        String asm = new AssemblyGenerator().generate(ir);
        for (String l : asm.split("\n")) {
            System.out.println("    " + l);
        }

        // ── [5b] Execucao na VM ───────────────────────────────────────────────
        titulo("[5b] Execucao na Maquina Virtual");

        // Se o programa tem instrucoes read, pede os valores ao usuario
        List<Object> inputs = coletarEntradas(ir);

        System.out.println("\n  Resultado da execucao:\n");
        List<String> saida = new VirtualMachine().execute(ir, inputs);

        if (saida.isEmpty()) {
            System.out.println("  (programa nao produziu saida)");
        } else {
            saida.forEach(l -> System.out.println("  >>> " + l));
        }

        System.out.println("\n" + SEP2);
        System.out.println("  [SUCESSO] Compilacao e execucao concluidas!");
        System.out.println(SEP2);
    }

    // -------------------------------------------------------------------------
    // Coleta de entradas para instrucoes read
    // -------------------------------------------------------------------------

    /**
     * Verifica quantos read o programa tem e pede os valores ao usuario.
     */
    private static List<Object> coletarEntradas(IrProgram ir) {
        List<Object> inputs = new ArrayList<>();
        long totalReads = ir.getInstructions().stream()
            .filter(i -> i.getOp() == TacInstruction.Op.READ)
            .count();

        if (totalReads == 0) return inputs;

        System.out.println("\n  O programa possui " + totalReads +
            " instrucao(oes) de leitura (read).");
        System.out.println("  Forneca os valores abaixo:\n");

        for (int i = 1; i <= totalReads; i++) {
            System.out.printf("  Valor %d: ", i);
            String raw = scanner.nextLine().trim();
            // Tenta converter para int; se falhar, guarda como string
            try {
                inputs.add(Integer.parseInt(raw));
            } catch (NumberFormatException e) {
                if (raw.equals("true"))       inputs.add(true);
                else if (raw.equals("false")) inputs.add(false);
                else                          inputs.add(raw);
            }
        }
        return inputs;
    }

    // -------------------------------------------------------------------------
    // Referencia da linguagem
    // -------------------------------------------------------------------------

    private static void ajuda() {
        System.out.println();
        System.out.println(SEP2);
        System.out.println("  REFERENCIA DA LINGUAGEM");
        System.out.println(SEP2);
        System.out.println();
        System.out.println("  TIPOS:");
        System.out.println("    int    -> numero inteiro         ex: int x = 10;");
        System.out.println("    bool   -> verdadeiro/falso       ex: bool b = true;");
        System.out.println("    string -> texto                  ex: string s = \"ola\";");
        System.out.println();
        System.out.println("  OPERADORES:");
        System.out.println("    Aritmeticos:   +  -  *  /");
        System.out.println("    Relacionais:   ==  !=  <  >  <=  >=");
        System.out.println("    Logicos:       &&  ||  !");
        System.out.println();
        System.out.println("  ESTRUTURAS:");
        System.out.println("    if (condicao) { ... }");
        System.out.println("    if (condicao) { ... } else { ... }");
        System.out.println("    while (condicao) { ... }");
        System.out.println();
        System.out.println("  ENTRADA / SAIDA:");
        System.out.println("    print(expressao);");
        System.out.println("    read(variavel);");
        System.out.println();
        System.out.println("  COMENTARIOS:");
        System.out.println("    // comentario de linha");
        System.out.println("    /* comentario de bloco */");
        System.out.println();
        System.out.println("  EXEMPLO:");
        System.out.println("    int soma = 0;");
        System.out.println("    int i = 1;");
        System.out.println("    while (i <= 10) {");
        System.out.println("        soma = soma + i;");
        System.out.println("        i = i + 1;");
        System.out.println("    }");
        System.out.println("    print(soma);");
        System.out.println();
        System.out.println(SEP);
    }

    // -------------------------------------------------------------------------
    // Helpers visuais
    // -------------------------------------------------------------------------

    private static void banner() {
        System.out.println();
        System.out.println("+" + SEP2 + "+");
        System.out.println("|                                                              |");
        System.out.println("|          COMPILADOR DIDATICO EM JAVA                        |");
        System.out.println("|          Lexico | Sintatico | Semantico | TAC | VM          |");
        System.out.println("|                                                              |");
        System.out.println("+" + SEP2 + "+");
        System.out.println();
        System.out.println("  Bem-vindo! Digite :ajuda para ver a referencia da linguagem.");
    }

    private static void titulo(String nome) {
        System.out.println("\n  " + SEP);
        System.out.println("  " + nome);
        System.out.println("  " + SEP);
    }

    private static void ok(String msg) {
        System.out.println("\n  [OK] " + msg);
    }

    private static void erro(String msg) {
        System.out.println("  [ERRO] " + msg);
    }

    private static void encerrarComErro(String dica) {
        System.out.println("\n  Dica: " + dica);
        System.out.println(SEP2);
    }

    private static void limparTela() {
        // Funciona no terminal do sistema operacional
        System.out.print("\033[H\033[2J");
        System.out.flush();
        banner();
    }
}
