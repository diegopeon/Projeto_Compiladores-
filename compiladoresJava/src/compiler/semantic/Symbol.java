package compiler.semantic;

/**
 * Representa uma entrada na Tabela de Símbolos.
 *
 * Cada variável declarada no programa vira um Symbol com:
 *   - nome  → como a variável foi chamada no código
 *   - tipo  → int, bool ou string
 *   - linha → onde foi declarada (para mensagens de erro claras)
 *
 * É um Value Object imutável — uma vez declarada, a entrada não muda.
 */
public class Symbol {

    /** Nome da variável como aparece no código-fonte. */
    private final String name;

    /** Tipo de dado da variável. */
    private final DataType type;

    /** Linha do código-fonte onde a variável foi declarada. */
    private final int declaredAtLine;

    /**
     * @param name           nome da variável
     * @param type           tipo de dado
     * @param declaredAtLine linha da declaração (para erros)
     */
    public Symbol(String name, DataType type, int declaredAtLine) {
        this.name           = name;
        this.type           = type;
        this.declaredAtLine = declaredAtLine;
    }

    public String   getName()           { return name;           }
    public DataType getType()           { return type;           }
    public int      getDeclaredAtLine() { return declaredAtLine; }

    @Override
    public String toString() {
        return String.format("Symbol{name='%s', type=%s, line=%d}",
            name, type, declaredAtLine);
    }
}
