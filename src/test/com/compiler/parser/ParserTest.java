package com.compiler.parser;

import com.compiler.exceptions.ParserException;
import com.compiler.ast.Expr;
import com.compiler.ast.Stmt;
import com.compiler.scanner.Scanner;
import com.compiler.scanner.Token;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    @Test
    void parsesEachSupportedUnaryOperator() {
        assertUnaryExpression("-value", Token.TokenType.MINUS);
        assertUnaryExpression("*value", Token.TokenType.STAR);
        assertUnaryExpression("&value", Token.TokenType.AMPERSAND);
        assertUnaryExpression("++value", Token.TokenType.PLUS_PLUS);
        assertUnaryExpression("--value", Token.TokenType.MINUS_MINUS);
    }

    @Test
    void parsesStackedUnaryOperatorsRecursively() {
        Expr expression = parseExpression("**value");

        Expr.Unary outer = assertInstanceOf(Expr.Unary.class, expression);
        assertEquals(Token.TokenType.STAR, outer.Unaryop().type());

        Expr.Unary inner = assertInstanceOf(Expr.Unary.class, outer.value());
        assertEquals(Token.TokenType.STAR, inner.Unaryop().type());

        Expr.Variable variable = assertInstanceOf(Expr.Variable.class, inner.value());
        assertEquals("value", variable.name().lexeme());
    }

    @Test
    void unaryOperatorsBindMoreTightlyThanMultiplication() {
        Expr expression = parseExpression("-left * **right");

        Expr.Binary multiplication = assertInstanceOf(Expr.Binary.class, expression);
        assertEquals(Token.TokenType.STAR, multiplication.operator().type());

        Expr.Unary left = assertInstanceOf(Expr.Unary.class, multiplication.left());
        assertEquals(Token.TokenType.MINUS, left.Unaryop().type());
        assertInstanceOf(Expr.Variable.class, left.value());

        Expr.Unary right = assertInstanceOf(Expr.Unary.class, multiplication.right());
        assertEquals(Token.TokenType.STAR, right.Unaryop().type());
        assertInstanceOf(Expr.Unary.class, right.value());
    }

    @Test
    void multiplicationBindsMoreTightlyThanAddition() {
        Expr expression = parseExpression("a + b * c");

        Expr.Binary addition = assertInstanceOf(Expr.Binary.class, expression);
        assertEquals(Token.TokenType.PLUS, addition.operator().type());
        assertVariable(addition.left(), "a");

        Expr.Binary multiplication = assertInstanceOf(Expr.Binary.class, addition.right());
        assertEquals(Token.TokenType.STAR, multiplication.operator().type());
        assertVariable(multiplication.left(), "b");
        assertVariable(multiplication.right(), "c");
    }

    @Test
    void parenthesesOverrideOperatorPrecedence() {
        Expr expression = parseExpression("(a + b) * c");

        Expr.Binary multiplication = assertInstanceOf(Expr.Binary.class, expression);
        assertEquals(Token.TokenType.STAR, multiplication.operator().type());

        Expr.Grouping grouping = assertInstanceOf(Expr.Grouping.class, multiplication.left());
        Expr.Binary addition = assertInstanceOf(Expr.Binary.class, grouping.expression());
        assertEquals(Token.TokenType.PLUS, addition.operator().type());
        assertVariable(addition.left(), "a");
        assertVariable(addition.right(), "b");
        assertVariable(multiplication.right(), "c");
    }

    @Test
    void binaryOperatorsAssociateFromLeftToRight() {
        Expr expression = parseExpression("a - b - c");

        Expr.Binary outerSubtraction = assertInstanceOf(Expr.Binary.class, expression);
        assertEquals(Token.TokenType.MINUS, outerSubtraction.operator().type());
        assertVariable(outerSubtraction.right(), "c");

        Expr.Binary innerSubtraction = assertInstanceOf(Expr.Binary.class, outerSubtraction.left());
        assertEquals(Token.TokenType.MINUS, innerSubtraction.operator().type());
        assertVariable(innerSubtraction.left(), "a");
        assertVariable(innerSubtraction.right(), "b");
    }

    @Test
    void assignmentChainsAssociateFromRightToLeft() {
        Expr expression = parseExpression("a = b = c");

        Expr.Assign outerAssignment = assertInstanceOf(Expr.Assign.class, expression);
        assertVariable(outerAssignment.target(), "a");

        Expr.Assign innerAssignment = assertInstanceOf(Expr.Assign.class, outerAssignment.value());
        assertVariable(innerAssignment.target(), "b");
        assertVariable(innerAssignment.value(), "c");
    }

    @Test
    void parsesDereferenceInAnIfCondition() {
        List<Stmt> statements = parseStatements("if (*x > 10) {}");

        Stmt.IfStatement ifStatement = assertInstanceOf(Stmt.IfStatement.class, statements.get(0));
        Expr.Binary condition = assertInstanceOf(Expr.Binary.class, ifStatement.condition());
        assertEquals(Token.TokenType.GREATER, condition.operator().type());

        Expr.Unary dereference = assertInstanceOf(Expr.Unary.class, condition.left());
        assertEquals(Token.TokenType.STAR, dereference.Unaryop().type());
        Expr.Variable variable = assertInstanceOf(Expr.Variable.class, dereference.value());
        assertEquals("x", variable.name().lexeme());
    }

    @Test
    void danglingElseAttachesToNearestIf() {
        List<Stmt> statements = parseStatements(
                "if (outer) if (inner) first = 1; else second = 2;"
        );

        Stmt.IfStatement outerIf = assertInstanceOf(Stmt.IfStatement.class, statements.get(0));
        assertNull(outerIf.elseBody());

        Stmt.IfStatement innerIf = assertInstanceOf(Stmt.IfStatement.class, outerIf.ifBody());
        assertInstanceOf(Stmt.AssignmentStatement.class, innerIf.ifBody());

        Stmt.AssignmentStatement elseAssignment =
                assertInstanceOf(Stmt.AssignmentStatement.class, innerIf.elseBody());
        Expr.Assign assignment = assertInstanceOf(Expr.Assign.class, elseAssignment.assignment());
        assertVariable(assignment.target(), "second");
    }

    @Test
    void parsesAddressOfInAVariableInitializer() {
        List<Stmt> statements = parseStatements("int *pointer = &value;");

        Stmt.VariableDeclarationStatement declaration =
                assertInstanceOf(Stmt.VariableDeclarationStatement.class, statements.get(0));
        assertEquals(Token.TokenType.INT, declaration.type().type());
        assertEquals(1, declaration.ptrDepth());
        assertEquals("pointer", declaration.name().lexeme());

        Expr.Unary addressOf = assertInstanceOf(Expr.Unary.class, declaration.value());
        assertEquals(Token.TokenType.AMPERSAND, addressOf.Unaryop().type());
        Expr.Variable variable = assertInstanceOf(Expr.Variable.class, addressOf.value());
        assertEquals("value", variable.name().lexeme());
    }

    @Test
    void rejectsExpressionsWithMissingOperandsOrClosingParentheses() {
        assertInvalidExpression("a +");
        assertInvalidExpression("*");
        assertInvalidExpression("(a + b");
    }

    @Test
    void missingSemicolonPointsToInsertionAfterPreviousStatement() {
        String source = "z /= 2\nz %= 2;";
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scan(), scanner.getReader());
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        PrintStream originalError = System.err;

        try (PrintStream capturedError = new PrintStream(errorOutput, true, StandardCharsets.UTF_8)) {
            System.setErr(capturedError);
            assertThrows(ParserException.class, parser::parse);
        } finally {
            System.setErr(originalError);
        }

        String diagnostic = errorOutput.toString(StandardCharsets.UTF_8);
        assertTrue(diagnostic.contains("z /= 2" + System.lineSeparator() + "      ^"));
        assertFalse(diagnostic.contains(System.lineSeparator() + "z %= 2;"));
    }

    private static void assertUnaryExpression(String source, Token.TokenType expectedOperator) {
        Expr expression = parseExpression(source);

        Expr.Unary unary = assertInstanceOf(Expr.Unary.class, expression);
        assertEquals(expectedOperator, unary.Unaryop().type());

        Expr.Variable variable = assertInstanceOf(Expr.Variable.class, unary.value());
        assertEquals("value", variable.name().lexeme());
    }

    private static void assertVariable(Expr expression, String expectedName) {
        Expr.Variable variable = assertInstanceOf(Expr.Variable.class, expression);
        assertEquals(expectedName, variable.name().lexeme());
    }

    private static void assertInvalidExpression(String source) {
        Scanner scanner = new Scanner(source);
        ExpressionParser expressionParser = new ExpressionParser(
                new TokenStream(new ArrayList<>(scanner.scan()))
        );
        assertFalse(scanner.hasErrors());
        assertThrows(ParserException.class, expressionParser::parseExpression);
    }

    private static Expr parseExpression(String source) {
        Scanner scanner = new Scanner(source);
        ExpressionParser expressionParser = new ExpressionParser(
                new TokenStream(new ArrayList<>(scanner.scan()))
        );
        assertFalse(scanner.hasErrors());
        return expressionParser.parseExpression();
    }

    private static List<Stmt> parseStatements(String source) {
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scan(), scanner.getReader());
        assertFalse(scanner.hasErrors());
        return parser.parse();
    }
}
