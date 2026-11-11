package com.compiler.parser;

import com.compiler.exceptions.ParserException;
import com.compiler.scanner.Scanner;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {
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
}
