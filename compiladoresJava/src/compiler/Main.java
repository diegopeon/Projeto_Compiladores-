package compiler;

import compiler.ast.stmt.ProgramNode;
import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.AstPrinter;
import compiler.parser.ParseException;
import compiler.parser.Parser;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticException;

import java.util.List;

/**
 * Ponto de entrada — executa as 3 etapas do compilador em sequência.
 *
 * Etapa 1: Análise Léxica    (Lexer  → tokens)
 * Etapa 2: Análise Sintática (Parser → AST)
 * Etapa 3: Análise Semântica (SemanticAnalyzer → validação de tipos e escopos)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Compilador — Etapas 1, 2 e 3 completas     ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        // ── Casos válidos ────────────────────────────────────────────────────

        testCase("Declaração e uso correto de variáveis",
            """
            int x = 10;
            int y = 20;
            int soma = x + y;
            print(soma);
            """
        );

        testCase("If-else com condição booleana",
            """
            int idade = 18;
            bool maior = idade >= 18;
            if (maior) {
                print("adulto");
            } else {
                print("menor");
            }
            """
        );

        testCase("Laço while corretamente tipado",
            """
            int soma = 0;
            int i = 1;
            while (i <= 10) {
                soma = soma + i;
                i = i + 1;
            }
            print(soma);
            """
        );

        testCase("Escopo aninhado — variável local ao bloco",
            """
            int x = 5;
            if (x > 0) {
                int y = x + 1;
                print(y);
            }
            print(x);
            """
        );

        testCase("Operadores lógicos encadeados",
            """
            bool a = true;
            bool b = false;
            bool resultado = a && !b;
            print(resultado);
            """
        );

        testCase("Read e print",
            """
            int n;
            read(n);
            print(n);
            """
        );

        // ── Casos com erro semântico ─────────────────────────────────────────

        testCase("ERRO — variável não declarada",
            """
            int x = 10;
            print(y);
            """
        );

        testCase("ERRO — variável declarada duas vezes no mesmo escopo",
            """
            int x = 1;
            int x = 2;
            """
        );

        testCase("ERRO — tipo incompatível na declaração",
            """
            int x = true;
            """
        );

        testCase("ERRO — tipo incompatível na atribuição",
            """
            int x = 10;
            x = false;
            """
        );

        testCase("ERRO — operação aritmética com bool",
            """
            bool b = true;
            int x = b + 1;
            """
        );

        testCase("ERRO — condição do if não é bool",
            """
            int x = 10;
            if (x) {
                print("erro");
            }
            """
        );

        testCase("ERRO — uso de variável fora do escopo",
            """
            int x = 5;
            if (x > 0) {
                int y = 10;
            }
            print(y);
            """
        );
    }

    // -------------------------------------------------------------------------
    // Infraestrutura de testes
    // -------------------------------------------------------------------------

    private static void testCase(String description, String sourceCode) {
        printHeader(description, sourceCode);

        // ── Etapa 1 ──────────────────────────────────────────────────────────
        System.out.println("│ 📌 ETAPA 1 — Léxico:");
        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        for (Token t : tokens) {
            if (t.isType(TokenType.EOF)) continue;
            String marker = t.isType(TokenType.UNKNOWN) ? " ⚠" : "";
            System.out.printf("│   %-20s  %-15s  linha %-2d col %d%s%n",
                t.getType(), "\"" + t.getLexeme() + "\"",
                t.getLine(), t.getColumn(), marker);
        }

        if (lexer.hasErrors()) {
            lexer.getErrors().forEach(e -> System.out.println("│  ⚠ " + e));
            printFooter(); return;
        }
        System.out.println("│  ✓ Sem erros léxicos.");

        // ── Etapa 2 ──────────────────────────────────────────────────────────
        System.out.println("│");
        System.out.println("│ 📌 ETAPA 2 — AST:");
        ProgramNode ast;
        try {
            ast = new Parser(tokens).parse();

            // Captura a saída do AstPrinter e prefixa cada linha com "│   "
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream old = System.out;
            System.setOut(new java.io.PrintStream(baos));
            new AstPrinter().print(ast);
            System.setOut(old);
            for (String line : baos.toString().split("\n")) {
                System.out.println("│   " + line);
            }
            System.out.println("│  ✓ AST válida.");

        } catch (ParseException e) {
            System.out.println("│  ✗ " + e.getMessage());
            printFooter(); return;
        }

        // ── Etapa 3 ──────────────────────────────────────────────────────────
        System.out.println("│");
        System.out.println("│ 📌 ETAPA 3 — Semântico:");
        try {
            new SemanticAnalyzer().analyze(ast);
            System.out.println("│  ✓ Sem erros semânticos! Programa correto.");
        } catch (SemanticException e) {
            System.out.println("│  ✗ " + e.getMessage());
        }

        printFooter();
    }

    private static void printHeader(String description, String sourceCode) {
        System.out.println("┌─────────────────────────────────────────");
        System.out.println("│ TESTE: " + description);
        System.out.println("├─────────────────────────────────────────");
        String[] lines = sourceCode.split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("│  %2d │ %s%n", i + 1, lines[i]);
        }
        System.out.println("├─────────────────────────────────────────");
    }

    private static void printFooter() {
        System.out.println("└─────────────────────────────────────────\n");
    }
}
