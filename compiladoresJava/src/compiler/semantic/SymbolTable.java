package compiler.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tabela de Símbolos com suporte a escopo aninhado.
 *
 * Implementada como uma pilha de mapas (Deque de HashMaps).
 * Cada nível da pilha representa um escopo:
 *
 *   Escopo global (nível 0)         → variáveis do programa principal
 *     Escopo do if (nível 1)        → variáveis do bloco then/else
 *       Escopo do while (nível 2)   → variáveis dentro do laço
 *
 * Quando um bloco { } é aberto → empilhamos um novo mapa (pushScope).
 * Quando um bloco { } é fechado → desempilhamos o mapa (popScope).
 * Isso garante que variáveis declaradas dentro de um bloco não
 * "vazem" para escopos externos.
 *
 * Exemplo:
 *   int x = 1;          // nível 0: {x}
 *   if (x > 0) {
 *       int y = 2;      // nível 1: {y}  x ainda acessível via lookup
 *   }
 *   print(y);           // ERRO — y não existe no nível 0
 */
public class SymbolTable {

    /**
     * Pilha de escopos. O topo (peek) é o escopo mais interno (atual).
     * Usamos Deque em vez de Stack porque Stack é legada e sincronizada.
     */
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    // -------------------------------------------------------------------------
    // Gerenciamento de escopos
    // -------------------------------------------------------------------------

    /**
     * Abre um novo escopo (chamado ao encontrar '{').
     * Empilha um novo mapa vazio no topo da pilha.
     */
    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    /**
     * Fecha o escopo atual (chamado ao encontrar '}').
     * Remove o mapa do topo, descartando todas as variáveis desse bloco.
     */
    public void popScope() {
        scopes.pop();
    }

    // -------------------------------------------------------------------------
    // Operações sobre símbolos
    // -------------------------------------------------------------------------

    /**
     * Declara uma nova variável no escopo atual.
     *
     * Verifica se já existe uma variável com o mesmo nome NO MESMO ESCOPO.
     * Não impede sombreamento de escopos externos (como a maioria das linguagens).
     *
     * @param symbol símbolo a registrar
     * @throws SemanticException se a variável já foi declarada neste escopo
     */
    public void declare(Symbol symbol) {
        Map<String, Symbol> currentScope = scopes.peek();

        // Verifica conflito apenas no escopo imediato
        if (currentScope != null && currentScope.containsKey(symbol.getName())) {
            Symbol existing = currentScope.get(symbol.getName());
            throw new SemanticException(String.format(
                "Erro semântico [linha %d]: variável '%s' já foi declarada neste escopo (declarada antes na linha %d)",
                symbol.getDeclaredAtLine(),
                symbol.getName(),
                existing.getDeclaredAtLine()
            ));
        }

        if (currentScope != null) {
            currentScope.put(symbol.getName(), symbol);
        }
    }

    /**
     * Busca uma variável pelo nome percorrendo os escopos do mais interno
     * ao mais externo (inner → outer), respeitando as regras de visibilidade.
     *
     * @param name nome da variável a buscar
     * @return o Symbol encontrado, ou null se não existir em nenhum escopo
     */
    public Symbol lookup(String name) {
        // Percorre a pilha do topo (escopo mais interno) para a base
        for (Map<String, Symbol> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // não encontrado em nenhum escopo
    }

    /**
     * Verifica se uma variável está acessível no escopo atual.
     *
     * @param name nome da variável
     * @return true se a variável existe em algum escopo visível
     */
    public boolean isDeclared(String name) {
        return lookup(name) != null;
    }

    /**
     * Retorna a profundidade atual de aninhamento de escopos.
     * Útil para depuração.
     */
    public int getScopeDepth() {
        return scopes.size();
    }
}
