package compiler.parser;

import compiler.ast.NodeVisitor;
import compiler.ast.expr.*;
import compiler.ast.stmt.*;

/**
 * Visitor que imprime a AST como uma árvore indentada.
 *
 * Útil para depuração — permite visualizar a estrutura hierárquica
 * produzida pelo parser antes de avançar para as fases seguintes.
 *
 * Cada nível de aninhamento é representado por um incremento de indentação,
 * usando os caracteres de árvore ├─, └─ e │ para legibilidade.
 *
 * Exemplo de saída para "int x = 2 + 3;":
 *
 *   ProgramNode
 *   └─ VarDeclStmt [int "x"]
 *      └─ BinaryExpr [+]
 *         ├─ IntLiteralExpr [2]
 *         └─ IntLiteralExpr [3]
 */
public class AstPrinter implements NodeVisitor<String> {

    /** Prefixo acumulado de indentação para o nó atual. */
    private String indent = "";

    /**
     * Ponto de entrada: imprime a AST completa a partir do nó raiz.
     *
     * @param program nó raiz retornado pelo Parser
     */
    public void print(ProgramNode program) {
        System.out.println(program.accept(this));
    }

    // -------------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------------

    @Override
    public String visitProgram(ProgramNode node) {
        StringBuilder sb = new StringBuilder("ProgramNode\n");
        var stmts = node.getStatements();
        for (int i = 0; i < stmts.size(); i++) {
            boolean last = (i == stmts.size() - 1);
            sb.append(childStr(stmts.get(i), last));
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public String visitBlock(BlockStmt node) {
        StringBuilder sb = new StringBuilder("BlockStmt\n");
        var stmts = node.getStatements();
        for (int i = 0; i < stmts.size(); i++) {
            boolean last = (i == stmts.size() - 1);
            sb.append(childStr(stmts.get(i), last));
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public String visitVarDecl(VarDeclStmt node) {
        String header = String.format("VarDeclStmt [%s \"%s\"]\n",
            node.getTypeToken().getLexeme(),
            node.getNameToken().getLexeme()
        );
        if (!node.hasInitializer()) return header.stripTrailing();

        return header + childStr(node.getInitializer(), true);
    }

    @Override
    public String visitAssign(AssignStmt node) {
        String header = String.format("AssignStmt [\"%s\"]\n",
            node.getNameToken().getLexeme()
        );
        return header + childStr(node.getValue(), true);
    }

    @Override
    public String visitIf(IfStmt node) {
        StringBuilder sb = new StringBuilder("IfStmt\n");
        // A condição nunca é o último filho se houver ramos
        sb.append(childStr(node.getCondition(), false));
        sb.append(indentLabel("then"));
        sb.append(childStr(node.getThenBranch(), !node.hasElse()));
        if (node.hasElse()) {
            sb.append(indentLabel("else"));
            sb.append(childStr(node.getElseBranch(), true));
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public String visitWhile(WhileStmt node) {
        StringBuilder sb = new StringBuilder("WhileStmt\n");
        sb.append(childStr(node.getCondition(), false));
        sb.append(indentLabel("body"));
        sb.append(childStr(node.getBody(), true));
        return sb.toString().stripTrailing();
    }

    @Override
    public String visitPrint(PrintStmt node) {
        return "PrintStmt\n" + childStr(node.getExpression(), true);
    }

    @Override
    public String visitRead(ReadStmt node) {
        return String.format("ReadStmt [\"%s\"]", node.getNameToken().getLexeme());
    }

    // -------------------------------------------------------------------------
    // Expressões
    // -------------------------------------------------------------------------

    @Override
    public String visitBinary(BinaryExpr node) {
        String header = String.format("BinaryExpr [%s]\n", node.getOperator().getLexeme());
        return header
            + childStr(node.getLeft(),  false)
            + childStr(node.getRight(), true);
    }

    @Override
    public String visitUnary(UnaryExpr node) {
        String header = String.format("UnaryExpr [%s]\n", node.getOperator().getLexeme());
        return header + childStr(node.getOperand(), true);
    }

    @Override
    public String visitIdentifier(IdentifierExpr node) {
        return String.format("IdentifierExpr [\"%s\"]", node.getName());
    }

    @Override
    public String visitIntLiteral(IntLiteralExpr node) {
        return String.format("IntLiteralExpr [%d]", node.getValue());
    }

    @Override
    public String visitBoolLiteral(BoolLiteralExpr node) {
        return String.format("BoolLiteralExpr [%b]", node.getValue());
    }

    @Override
    public String visitStringLiteral(StringLiteralExpr node) {
        return String.format("StringLiteralExpr [\"%s\"]", node.getValue());
    }

    // -------------------------------------------------------------------------
    // Helpers de formatação da árvore
    // -------------------------------------------------------------------------

    /**
     * Formata um nó filho com o conector de árvore correto e indentação.
     *
     * @param node    nó filho a formatar
     * @param isLast  true se for o último filho (usa └─ em vez de ├─)
     * @return string formatada com conector + conteúdo do nó
     */
    private String childStr(compiler.ast.Node node, boolean isLast) {
        String connector  = isLast ? "└─ " : "├─ ";
        String childIndent = isLast ? "   " : "│  ";

        String savedIndent = indent;
        String result = indent + connector + withIndent(node, childIndent);
        indent = savedIndent;

        return result + "\n";
    }

    /**
     * Visita um nó com a indentação temporariamente aumentada.
     */
    private String withIndent(compiler.ast.Node node, String extra) {
        indent = indent + extra;
        String result = node.accept(this);
        // A indentação é restaurada em childStr após a chamada
        return result;
    }

    /**
     * Formata um rótulo de seção (ex: "then:", "body:") na indentação atual.
     */
    private String indentLabel(String label) {
        return indent + "│  [" + label + "]\n";
    }
}
