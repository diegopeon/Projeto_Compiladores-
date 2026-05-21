package compiler;

import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

import java.util.List;

/**
 * Ponto de entrada da aplicação — usado para testar o compilador por etapas.
 *
 * A cada nova fase do compilador (parser, semântico, geração de código),
 * adicionaremos chamadas aqui para validar a integração entre os módulos.
 *
 * Por enquanto,Etapa 1.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Compilador — Teste da Etapa 1: Análise Léxica ===\n");

        // Rodamos uma bateria de casos de teste em sequência
        testCase("Declaração e atribuição simples",
            "int x = 42;"
        );

        testCase("Estrutura if-else completa",
            """
            int idade = 18;
            if (idade >= 18) {
                print("maior de idade");
            } else {
                print("menor de idade");
            }
            """
        );

        testCase("Laço while com operações aritméticas",
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
            bool admin  = false;
            if (ativo && !admin) {
                print("usuário comum ativo");
            }
            """
        );

        testCase("Comentários de linha e bloco (devem ser ignorados)",
            """
            // Este comentário deve desaparecer
            int x = 10; // comentário no fim da linha
            /* Este também
               é um comentário
               de múltiplas linhas */
            int y = 20;
            """
        );

        testCase("Operadores relacionais e de igualdade",
            "bool ok = (x == 5) || (y != 3) && (z > 0);"
        );

        testCase("Entrada de dados com read",
            """
            int n;
            read(n);
            print(n);
            """
        );

        testCase("Erros léxicos — caracteres inválidos",
            """
            int x = 10;
            int y = x @ 2;
            int z = x # y;
            """
        );

        testCase("String não terminada — erro léxico",
            "print(\"olá mundo);"
        );
    }

    // -------------------------------------------------------------------------
    // Infraestrutura de testes
    // -------------------------------------------------------------------------

    /**
     * Executa o lexer sobre o código fornecido e exibe o resultado formatado.
     *
     * Mostra os tokens gerados e, se houver erros léxicos, os lista separadamente
     * ao final — simulando o comportamento de um compilador real que coleta
     * todos os erros antes de abortar.
     *
     * @param description título amigável do caso de teste
     * @param sourceCode  código-fonte a ser analisado
     */
    private static void testCase(String description, String sourceCode) {
        // Cabeçalho do caso de teste
        System.out.println("┌─────────────────────────────────────────");
        System.out.println("│ TESTE: " + description);
        System.out.println("├─────────────────────────────────────────");

        // Exibe o código-fonte com numeração de linhas
        System.out.println("│ Código-fonte:");
        String[] lines = sourceCode.split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("│  %2d │ %s%n", i + 1, lines[i]);
        }
        System.out.println("├─────────────────────────────────────────");

        // Executa o lexer
        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        // Exibe os tokens produzidos (exceto EOF para não poluir a saída)
        System.out.println("│ Tokens produzidos:");
        for (Token token : tokens) {
            if (token.isType(TokenType.EOF)) continue;

            // Tokens com erro são destacados visualmente
            String marker = token.isType(TokenType.UNKNOWN) ? " ⚠" : "";
            System.out.printf("│   %-20s  %-15s  linha %-3d col %d%s%n",
                token.getType(),
                "\"" + token.getLexeme() + "\"",
                token.getLine(),
                token.getColumn(),
                marker
            );
        }

        // Exibe erros léxicos, se houver
        if (lexer.hasErrors()) {
            System.out.println("├─────────────────────────────────────────");
            System.out.println("│ ⚠  Erros léxicos encontrados:");
            for (String error : lexer.getErrors()) {
                System.out.println("│   → " + error);
            }
        } else {
            System.out.println("│ ✓  Nenhum erro léxico encontrado.");
        }

        System.out.println("└─────────────────────────────────────────");
        System.out.println();
    }
}