package compiler;

import compiler.ast.stmt.ProgramNode;
import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.AstPrinter;
import compiler.parser.ParseException;
import compiler.parser.Parser;

import java.util.List;

/**
 * Ponto de entrada do compilador — executa as etapas 1 e 2 em sequência.
 *
 * Para cada caso de teste, exibe:
 *   1. O código-fonte original
 *   2. Os tokens produzidos pelo Lexer  (Etapa 1)
 *   3. A AST produzida pelo Parser      (Etapa 2)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Compilador — Etapas 1 e 2 completas    ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        testCase("Declaração e atribuição simples",
            "int x = 42;"
        );

        testCase("Expressão aritmética — teste de precedência",
            "int resultado = 2 + 3 * 4;"
        );

        testCase("If-else completo",
            """
            int idade = 18;
            if (idade >= 18) {
                print("maior de idade");
            } else {
                print("menor de idade");
            }
            """
        );

        testCase("Laço while com múltiplas instruções",
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

        testCase("Operadores lógicos e booleanos",
            """
            bool ativo = true;
            bool admin = false;
            if (ativo && !admin) {
                print("usuário comum");
            }
            """
        );

        testCase("Parênteses invertendo precedência",
            "int x = (2 + 3) * (10 - 4);"
        );

        testCase("Read e print",
            """
            int n;
            read(n);
            print(n);
            """
        );

        testCase("Erro léxico — caractere inválido",
            "int x = 10 @ 2;"
        );

        testCase("Erro sintático — falta o ';'",
            "int x = 10"
        );

        testCase("Erro sintático — falta o ')' no if",
            "if (x > 0 { print(x); }"
        );
    }

    // -------------------------------------------------------------------------
    // Infraestrutura de testes
    // -------------------------------------------------------------------------

    /**
     * Executa as duas etapas sobre o código-fonte e exibe os resultados.
     * Se a Etapa 1 encontrar erros, a Etapa 2 não é executada.
     */
    private static void testCase(String description, String sourceCode) {
        printHeader(description, sourceCode);

        // ── Etapa 1: Análise Léxica ──────────────────────────────────────────
        System.out.println("│ 📌 ETAPA 1 — Tokens:");

        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            if (token.isType(TokenType.EOF)) continue;
            String marker = token.isType(TokenType.UNKNOWN) ? " ⚠" : "";
            System.out.printf("│   %-20s  %-15s  linha %-2d col %d%s%n",
                token.getType(),
                "\"" + token.getLexeme() + "\"",
                token.getLine(),
                token.getColumn(),
                marker
            );
        }

        if (lexer.hasErrors()) {
            System.out.println("│");
            System.out.println("│ ⚠  Erros léxicos — Etapa 2 não executada:");
            lexer.getErrors().forEach(e -> System.out.println("│   → " + e));
            printFooter();
            return;
        }

        System.out.println("│ ✓  Sem erros léxicos.");

        // ── Etapa 2: Análise Sintática ───────────────────────────────────────
        System.out.println("│");
        System.out.println("│ 📌 ETAPA 2 — AST:");

        try {
            Parser parser = new Parser(tokens);
            ProgramNode ast = parser.parse();
            System.out.println("│ ✓  AST gerada com sucesso:");
            System.out.println("│");

            // Imprime cada linha da AST prefixada com "│   "
            AstPrinter printer = new AstPrinter();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream old = System.out;
            System.setOut(new java.io.PrintStream(baos));
            printer.print(ast);
            System.setOut(old);
            for (String line : baos.toString().split("\n")) {
                System.out.println("│   " + line);
            }

        } catch (ParseException e) {
            System.out.println("│ ✗  " + e.getMessage());
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

