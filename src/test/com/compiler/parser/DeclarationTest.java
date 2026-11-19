package com.compiler.parser;

import com.compiler.exceptions.ParserException;
import com.compiler.scanner.Scanner;
import com.compiler.scanner.Token;
import com.compiler.ast.Expr;
import com.compiler.ast.Stmt;
import com.compiler.ast.FunctionBody;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclarationTest {
    @Test
    void parsesVariableDeclarationsWithModifiersPointersAndOptionalValues() {
        List<Stmt> statements = parse(
                """
                static const int *count = 1 + 2;
                char character;
                int integer;
                float fraction;
                double precision;
                long distance;
                string name;
                void nothing;
                bool flag;
                auto inferred;
                bool enabled = 1;
                dynamic float *ratio;
                """
        );

        assertEquals(12, statements.size());

        Stmt.VariableDeclarationStatement count =
                assertInstanceOf(Stmt.VariableDeclarationStatement.class, statements.get(0));
        assertEquals(List.of("static", "const"), count.modifiers().stream().map(Token::lexeme).toList());
        assertEquals(Token.TokenType.INT, count.type().type());
        assertEquals(1, count.ptrDepth());
        assertEquals("count", count.name().lexeme());
        assertInstanceOf(Expr.Binary.class, count.value());

        List<Token.TokenType> baseTypes = List.of(
                Token.TokenType.CHAR,
                Token.TokenType.INT,
                Token.TokenType.FLOAT,
                Token.TokenType.DOUBLE,
                Token.TokenType.LONG,
                Token.TokenType.STRING,
                Token.TokenType.VOID,
                Token.TokenType.BOOL,
                Token.TokenType.AUTO
        );
        for (int i = 0; i < baseTypes.size(); i++) {
            Stmt.VariableDeclarationStatement declaration =
                    assertInstanceOf(
                            Stmt.VariableDeclarationStatement.class,
                            statements.get(i + 1)
                    );
            assertEquals(baseTypes.get(i), declaration.type().type());
            assertNull(declaration.value());
        }

        Stmt.VariableDeclarationStatement enabled =
                assertInstanceOf(Stmt.VariableDeclarationStatement.class, statements.get(10));
        assertEquals(Token.TokenType.BOOL, enabled.type().type());
        assertEquals("enabled", enabled.name().lexeme());
        assertInstanceOf(Expr.Literal.class, enabled.value());

        Stmt.VariableDeclarationStatement ratio =
                assertInstanceOf(Stmt.VariableDeclarationStatement.class, statements.get(11));
        assertEquals(List.of("dynamic"), ratio.modifiers().stream().map(Token::lexeme).toList());
        assertEquals(Token.TokenType.FLOAT, ratio.type().type());
        assertEquals(1, ratio.ptrDepth());
        assertEquals("ratio", ratio.name().lexeme());
        assertNull(ratio.value());
    }

    @Test
    void parsesFunctionsWithParametersAndBothSupportedBodyForms() {
        List<Stmt> statements = parse("""
                void empty() {}
                static double **scale(int *value, double factor = 2.5) -> value * factor;
                int increment(int value) { int next = value + 1; }
                """);

        assertEquals(3, statements.size());

        Stmt.FunctionDeclarationStatement empty =
                assertInstanceOf(Stmt.FunctionDeclarationStatement.class, statements.get(0));
        assertEquals(Token.TokenType.VOID, empty.type().type());
        assertEquals("empty", empty.name().lexeme());
        assertTrue(empty.parameters().isEmpty());
        assertInstanceOf(FunctionBody.BlockBody.class, empty.body());

        Stmt.FunctionDeclarationStatement scale =
                assertInstanceOf(Stmt.FunctionDeclarationStatement.class, statements.get(1));
        assertEquals(List.of("static"), scale.modifiers().stream().map(Token::lexeme).toList());
        assertEquals(Token.TokenType.DOUBLE, scale.type().type());
        assertEquals(2, scale.ptrDepth());
        assertEquals("scale", scale.name().lexeme());
        assertEquals(2, scale.parameters().size());
        assertEquals(1, scale.parameters().get(0).ptrDepth());
        assertEquals("value", scale.parameters().get(0).name().lexeme());
        assertNull(scale.parameters().get(0).defaultValue());
        assertEquals("factor", scale.parameters().get(1).name().lexeme());
        assertInstanceOf(Expr.Literal.class, scale.parameters().get(1).defaultValue());
        assertInstanceOf(FunctionBody.ExpressionBody.class, scale.body());

        Stmt.FunctionDeclarationStatement increment =
                assertInstanceOf(Stmt.FunctionDeclarationStatement.class, statements.get(2));
        FunctionBody.BlockBody body =
                assertInstanceOf(FunctionBody.BlockBody.class, increment.body());
        assertEquals(1, body.block().statements().size());
        assertInstanceOf(
                Stmt.VariableDeclarationStatement.class,
                body.block().statements().get(0)
        );
    }

    @Test
    void rejectsMalformedVariableAndFunctionDeclarations() {
        assertInvalidDeclaration("int value = 1");
        assertInvalidDeclaration("int invalid(int) {}");
        assertInvalidDeclaration("int invalid() 42;");
    }

    private static List<Stmt> parse(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scan();
        assertFalse(scanner.hasErrors());
        return new Parser(tokens, scanner.getReader()).parse();
    }

    private static void assertInvalidDeclaration(String source) {
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        PrintStream originalError = System.err;

        try (PrintStream capturedError = new PrintStream(errorOutput, true, StandardCharsets.UTF_8)) {
            System.setErr(capturedError);
            assertThrows(ParserException.class, () -> parse(source));
        } finally {
            System.setErr(originalError);
        }
    }
}
