package compiler.ast;

import compiler.ast.expr.*;
import compiler.ast.stmt.*;

/**
 * Interface do Visitor Pattern para percorrer a AST.
 *
 * Cada fase posterior do compilador (semântico, geração de IR, etc.)
 * implementará esta interface sem precisar modificar os nós da AST.
 * Isso respeita o princípio Open/Closed: aberto para extensão,
 * fechado para modificação.
 *
 * Como usar:
 *   Crie uma classe que implemente NodeVisitor<T> onde T é o tipo
 *   que a operação retorna. Por exemplo:
 *     - AstPrinter  implementa NodeVisitor<String>  (retorna texto formatado)
 *     - TypeChecker implementa NodeVisitor<DataType> (retorna o tipo inferido)
 *     - IrGenerator implementa NodeVisitor<String>  (retorna código TAC)
 *
 * @param <T> tipo de retorno de cada visita
 */
public interface NodeVisitor<T> {

    // -------------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------------

    T visitProgram(ProgramNode node);
    T visitBlock(BlockStmt node);
    T visitVarDecl(VarDeclStmt node);
    T visitAssign(AssignStmt node);
    T visitIf(IfStmt node);
    T visitWhile(WhileStmt node);
    T visitPrint(PrintStmt node);
    T visitRead(ReadStmt node);

    // -------------------------------------------------------------------------
    // Expressões
    // -------------------------------------------------------------------------

    T visitBinary(BinaryExpr node);
    T visitUnary(UnaryExpr node);
    T visitIdentifier(IdentifierExpr node);
    T visitIntLiteral(IntLiteralExpr node);
    T visitBoolLiteral(BoolLiteralExpr node);
    T visitStringLiteral(StringLiteralExpr node);
}
