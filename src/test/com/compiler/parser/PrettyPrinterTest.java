package com.compiler.parser;

import com.compiler.scanner.Token;
import com.compiler.ast.Expr;
import com.compiler.ast.Stmt;
import com.compiler.ast.FunctionBody;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properly displays AST trees as tree graphics onto the terminal (from top to bottom)
 *
 * @author Copilot
 */
class PrettyPrinterTest {
    @Test
    void formatsBlocksAndAssignmentStatements() {
        Expr.Variable target = variable("x");
        Stmt assignment = new Stmt.AssignmentStatement(
                new Expr.Assign(
                        target,
                        operator(Token.TokenType.EQUAL, "="),
                        new Expr.Literal(10)
                )
        );

        String output = PrettyPrinter.format(new Stmt.Block(List.of(assignment)));

        assertTrue(output.contains("Block"));
        assertTrue(output.contains("AssignmentStatement"));
        assertTrue(output.contains("x"));
        assertTrue(output.contains("10"));
        assertTreeAligned(output);
    }

    @Test
    void formatsIfStatementsWithOptionalElseAndNestedBlocks() {
        Stmt thenBlock = new Stmt.Block(List.of(
                new Stmt.AssignmentStatement(
                        new Expr.Assign(
                                variable("x"),
                                operator(Token.TokenType.EQUAL, "="),
                                new Expr.Literal(1)
                        )
                )
        ));
        Stmt elseStatement = new Stmt.AssignmentStatement(
                new Expr.Assign(
                        variable("x"),
                        operator(Token.TokenType.PLUS_EQUAL, "+="),
                        new Expr.Literal(2)
                )
        );
        Stmt ifStatement = new Stmt.IfStatement(
                new Expr.Binary(
                        variable("x"),
                        Token.simple(Token.TokenType.GREATER, ">", new Token.Location(1, 1)),
                        new Expr.Literal(0)
                ),
                thenBlock,
                elseStatement
        );

        String output = PrettyPrinter.format(ifStatement);

        assertTrue(output.contains("IfStatement"));
        assertTrue(output.contains("Condition"));
        assertTrue(output.contains("Then"));
        assertTrue(output.contains("Else"));
        assertTrue(output.contains("Block"));
        assertTrue(output.contains("AssignmentStatement"));
        assertTrue(output.contains(">"));
        assertTrue(output.contains("+="));
        assertTreeAligned(output);
    }

    @Test
    void formatsEmptyBlocksAndIfStatementsWithoutElse() {
        assertTrue(PrettyPrinter.format(new Stmt.Block(List.of())).contains("Block"));

        Stmt ifStatement = new Stmt.IfStatement(
                new Expr.Literal(true),
                new Stmt.Block(List.of()),
                null
        );
        String output = PrettyPrinter.format(ifStatement);

        assertTrue(output.contains("IfStatement"));
        assertTrue(output.contains("Condition"));
        assertTrue(output.contains("Then"));
        assertTreeAligned(output);
    }

    @Test
    void formatsNestedUnaryExpressions() {
        Expr expression = new Expr.Unary(
                operator(Token.TokenType.PLUS_PLUS, "++"),
                new Expr.Unary(
                        operator(Token.TokenType.STAR, "*"),
                        new Expr.Unary(
                                operator(Token.TokenType.AMPERSAND, "&"),
                                variable("value")
                        )
                )
        );

        String output = PrettyPrinter.format(expression);

        assertTrue(output.contains("Unary: ++"));
        assertTrue(output.contains("Unary: *"));
        assertTrue(output.contains("Unary: &"));
        assertTrue(output.contains("value"));
        assertTreeAligned(output);
    }

    @Test
    void formatsAssignmentToDereferencedTarget() {
        Expr expression = new Expr.Assign(
                new Expr.Unary(operator(Token.TokenType.STAR, "*"), variable("pointer")),
                operator(Token.TokenType.EQUAL, "="),
                new Expr.Literal(3)
        );

        String output = PrettyPrinter.format(new Stmt.AssignmentStatement(expression));

        assertTrue(output.contains("AssignmentStatement"));
        assertTrue(output.contains("="));
        assertTrue(output.contains("Unary: *"));
        assertTrue(output.contains("pointer"));
        assertTrue(output.contains("3"));
        assertTreeAligned(output);
    }

    @Test
    void formatsUnaryExpressionStatements() {
        Stmt statement = new Stmt.AssignmentStatement(
                new Expr.Unary(operator(Token.TokenType.PLUS_PLUS, "++"), variable("value"))
        );

        String output = PrettyPrinter.format(statement);

        assertTrue(output.contains("AssignmentStatement"));
        assertTrue(output.contains("Unary: ++"));
        assertTrue(output.contains("value"));
        assertTreeAligned(output);
    }

    private static void assertTreeAligned(String output) {
        assertTrue(output.lines().map(String::length).distinct().count() == 1);
    }

    private static Expr.Variable variable(String name) {
        return new Expr.Variable(
                Token.simple(Token.TokenType.IDENTIFIER, name, new Token.Location(1, 1))
        );
    }

    private static Token operator(Token.TokenType type, String lexeme) {
        return Token.simple(type, lexeme, new Token.Location(1, 1));
    }
}
