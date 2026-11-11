package com.compiler.parser;

import com.compiler.scanner.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properly displays AST trees as tree graphics onto the terminal (from top to bottom)
 * @author Copilot
 */
class PrettyPrinterTest {
    @Test
    void formatsBlocksAndAssignmentStatements() {
        Parser.Expr.Variable target = variable("x");
        Parser.Stmt assignment = new Parser.Stmt.AssignmentStatement(
                new Parser.Expr.Assign(
                        target,
                        operator(Token.TokenType.EQUAL, "="),
                        new Parser.Expr.Literal(10)
                )
        );

        String output = PrettyPrinter.format(new Parser.Stmt.Block(List.of(assignment)));

        assertTrue(output.contains("Block"));
        assertTrue(output.contains("AssignmentStatement"));
        assertTrue(output.contains("x"));
        assertTrue(output.contains("10"));
        assertTreeAligned(output);
    }

    @Test
    void formatsIfStatementsWithOptionalElseAndNestedBlocks() {
        Parser.Stmt thenBlock = new Parser.Stmt.Block(List.of(
                new Parser.Stmt.AssignmentStatement(
                        new Parser.Expr.Assign(
                                variable("x"),
                                operator(Token.TokenType.EQUAL, "="),
                                new Parser.Expr.Literal(1)
                        )
                )
        ));
        Parser.Stmt elseStatement = new Parser.Stmt.AssignmentStatement(
                new Parser.Expr.Assign(
                        variable("x"),
                        operator(Token.TokenType.PLUS_EQUAL, "+="),
                        new Parser.Expr.Literal(2)
                )
        );
        Parser.Stmt ifStatement = new Parser.Stmt.IfStatement(
                new Parser.Expr.Binary(
                        variable("x"),
                        Token.simple(Token.TokenType.GREATER, ">", new Token.Location(1, 1)),
                        new Parser.Expr.Literal(0)
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
        assertTrue(PrettyPrinter.format(new Parser.Stmt.Block(List.of())).contains("Block"));

        Parser.Stmt ifStatement = new Parser.Stmt.IfStatement(
                new Parser.Expr.Literal(true),
                new Parser.Stmt.Block(List.of()),
                null
        );
        String output = PrettyPrinter.format(ifStatement);

        assertTrue(output.contains("IfStatement"));
        assertTrue(output.contains("Condition"));
        assertTrue(output.contains("Then"));
        assertTreeAligned(output);
    }

    private static void assertTreeAligned(String output) {
        assertTrue(output.lines().map(String::length).distinct().count() == 1);
    }

    private static Parser.Expr.Variable variable(String name) {
        return new Parser.Expr.Variable(
                Token.simple(Token.TokenType.IDENTIFIER, name, new Token.Location(1, 1))
        );
    }

    private static Token operator(Token.TokenType type, String lexeme) {
        return Token.simple(type, lexeme, new Token.Location(1, 1));
    }
}
