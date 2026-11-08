package com.compiler.scanner;

import com.compiler.exceptions.ScannerException;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScannerTest {

    @Test
    void testBasicKeywordsAndIdentifiers() {
        String source = "string hello = \"World\";";
        Scanner scanner = new Scanner(source);

        // Scan line by line (or you can loop until EOF)
        List<Token> tokens = scanner.scanLine();

        assertEquals(5, tokens.size());
        assertEquals(Token.TokenType.STRING, tokens.get(0).type());
        assertEquals("string", tokens.get(0).lexeme());

        assertEquals(Token.TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("hello", tokens.get(1).lexeme());

        assertEquals(Token.TokenType.EQUAL, tokens.get(2).type());

        assertEquals(Token.TokenType.STRING_LITERAL, tokens.get(3).type());
        assertEquals("World", tokens.get(3).lexeme());
        assertEquals("World", tokens.get(3).literal());

        assertEquals(Token.TokenType.SEMICOLON, tokens.get(4).type());
    }

    @Test
    void testNumericLiterals() {
        String source = "int x = 42; \ndouble pi = 3.14;";
        Scanner scanner = new Scanner(source);

        List<Token> line1 = scanner.scanLine();
        // int x = 42;
        assertEquals(Token.TokenType.INT, line1.get(0).type());
        assertEquals(Token.TokenType.IDENTIFIER, line1.get(1).type());      // A check for the IDENTIFIER token
        assertEquals("x", line1.get(1).lexeme());                  // A check for the correct identifier name
        assertEquals(Token.TokenType.EQUAL, line1.get(2).type());           // A check for th EQUAL token
        assertEquals(Token.TokenType.INTEGER_LITERAL, line1.get(3).type()); // A check for the INTEGER_LITERAL token
        assertEquals(42, line1.get(3).literal());                   // A check for the actual literal (42)
        assertEquals(Token.TokenType.SEMICOLON, line1.get(4).type());       // A check for the semicolon

        List<Token> line2 = scanner.scanLine();
        // double pi = 3.14;
        assertEquals(Token.TokenType.DOUBLE, line2.get(0).type());          // A check for the DOUBLE TokenType
        assertEquals(Token.TokenType.FLOAT_LITERAL, line2.get(3).type());   // A check for the FLOAT_LITERAL TokenType
        assertEquals(3.14, line2.get(3).literal());                // A check for the actual literal (3.14)
        assertEquals(Token.TokenType.SEMICOLON, line2.get(4).type());      // A check for the semicolon
    }

    @Test
    void testOperatorsAndPunctuation() {
        String source = "++ -- += -> == <= && ||";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanLine();

        assertEquals(Token.TokenType.PLUS_PLUS, tokens.get(0).type());
        assertEquals(Token.TokenType.MINUS_MINUS, tokens.get(1).type());
        assertEquals(Token.TokenType.PLUS_EQUAL, tokens.get(2).type());
        assertEquals(Token.TokenType.ARROW_DASH, tokens.get(3).type());
        assertEquals(Token.TokenType.EQUAL_EQUAL, tokens.get(4).type());
        assertEquals(Token.TokenType.LESS_EQUAL, tokens.get(5).type());
        assertEquals(Token.TokenType.AND, tokens.get(6).type());
        assertEquals(Token.TokenType.OR, tokens.get(7).type());
    }

    @Test
    void testMalformedFloatingPointException() {
        String source = "double bad = 33.a;";
        Scanner scanner = new Scanner(source);

        // First line contains the malformed float (method reference used instead of lambda
        ScannerException ex = assertThrows(ScannerException.class, scanner::scanLine);

        assertTrue(ex.getMessage().contains("Malformed floating point literal"));
        assertEquals(1, ex.getLineNumber());
        assertTrue(scanner.hasErrors());
    }

    @Test
    void testUnterminatedStringException() {
        String source = "string unterm = \"Hello, World;";
        Scanner scanner = new Scanner(source);

        // Method reference used instead of lambda
        ScannerException ex = assertThrows(ScannerException.class, scanner::scanLine);

        assertTrue(ex.getMessage().contains("Unterminated String Literal") ||
                   ex.getMessage().contains("Unterminated string literal"));
        assertTrue(scanner.hasErrors());
    }

    @Test
    void testFullScanRecordsErrors() {
        Scanner scanner = new Scanner("@");

        List<Token> tokens = scanner.scan();

        assertTrue(scanner.hasErrors());
        assertEquals(Token.TokenType.EOF, tokens.get(tokens.size() - 1).type());
    }
}