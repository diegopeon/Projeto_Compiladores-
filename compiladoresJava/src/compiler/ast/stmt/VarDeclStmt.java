package compiler.ast.stmt;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.Expr;
import compiler.lexer.Token;

/**
 * Declaração de variável: tipo nome [ = expressão ] ;
 *
 * Exemplos:
 *   int x;
 *   int x = 42;
 *   bool ativo = true;
 *   string nome = "Maria";
 *
 * O inicializador é opcional — quando ausente, {@link #initializer} é null
 * e o analisador semântico pode atribuir um valor padrão ou exigir
 * atribuição antes do uso.
 */
public class VarDeclStmt extends Stmt {

    /** Token do tipo da variável (ex: KW_INT, KW_BOOL, KW_STRING). */
    private final Token typeToken;

    /** Token com o nome da variável (IDENTIFIER). */
    private final Token nameToken;

    /**
     * Expressão de inicialização, ou null se não houver.
     * Ex: em "int x = 2 + 3", o initializer é BinaryExpr(+, 2, 3).
     */
    private final Expr initializer;

    /**
     * @param typeToken   token do tipo declarado
     * @param nameToken   token com o nome da variável
     * @param initializer expressão inicial, ou null
     */
    public VarDeclStmt(Token typeToken, Token nameToken, Expr initializer) {
        this.typeToken   = typeToken;
        this.nameToken   = nameToken;
        this.initializer = initializer;
    }

    public Token getTypeToken()   { return typeToken;   }
    public Token getNameToken()   { return nameToken;   }
    public Expr  getInitializer() { return initializer; }

    /** @return true se a variável foi declarada com valor inicial */
    public boolean hasInitializer() {
        return initializer != null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitVarDecl(this);
    }
}
