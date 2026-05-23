package compiler.ast;

/**
 * Interface marcadora para todos os nós da Árvore de Sintaxe Abstrata (AST).
 *
 * Todo nó — seja um statement ou uma expressão — implementa esta interface,
 * o que nos permite tratar qualquer parte da árvore de forma uniforme.
 *
 * O método {@link #accept} é o contrato do Visitor Pattern:
 * cada nó concreto o implementa chamando o método correto do visitor,
 * permitindo percorrer a árvore sem usar instanceof ou casting.
 *
 * Teoria aplicada: a AST representa a estrutura hierárquica do programa
 * gerada pela Gramática Livre de Contexto (GLC) do parser.
 */
public interface Node {

    /**
     * Aceita um visitor e delega a execução para o método correspondente.
     *
     * @param <T>     tipo de retorno da operação do visitor
     * @param visitor o visitor que processará este nó
     * @return o resultado da operação definida pelo visitor
     */
    <T> T accept(NodeVisitor<T> visitor);
}
