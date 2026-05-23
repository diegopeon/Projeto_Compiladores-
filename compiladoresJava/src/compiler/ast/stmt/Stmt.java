package compiler.ast.stmt;

import compiler.ast.Node;

/**
 * Classe base abstrata para todos os nós de instrução (statements).
 *
 * Um statement é uma instrução completa que produz um efeito colateral
 * (declaração de variável, atribuição, controle de fluxo, E/S).
 * Ao contrário das expressões, statements não retornam um valor.
 *
 * Exemplos: "int x = 5;", "if (x > 0) { ... }", "print(x);"
 */
public abstract class Stmt implements Node {
    // Classe marcadora — toda lógica fica nas subclasses.
    // Centraliza o tipo para facilitar checagens futuras (instanceof Stmt).
}
