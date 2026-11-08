package com.compiler.exceptions;

import com.compiler.scanner.Token;
import com.compiler.scanner.SourceReader;

/**
 * Thrown by the scanner to indicate a lexical violation or malformed token
 * encountered during source code analysis.
 *
 * <p>This exception is unchecked (extending {@link RuntimeException}) to keep
 * the scanner's core character-processing loop clean of checked-exception
 * boilerplate, allowing lexical errors to cleanly bubble up to the compiler driver.
 *
 * <p>It encapsulates both a descriptive error message and structured
 * {@link Token.Location} metadata, enabling both human-readable console output
 * and programmatic coordinate retrieval for diagnostics or IDE integrations.
 *
 * @author Nicholas Griebel
 * @since 1.0
 */
public class ScannerException extends RuntimeException
{
    private final Token.Location location;
    private final int tokenLength;

    public ScannerException(String message, Token.Location location, int tokenLength)
    {
        super(message + " -> [Line " + location.line() + ", Char " + location._char() + "]");
        this.location = location;
        this.tokenLength = tokenLength;
    }

    public void printDiagnostic(SourceReader reader)
    {
        System.err.println(reader.getLine(location.line()));
        System.err.println(
                  " ".repeat(Math.max(0, location._char() - 1))
                + "^".repeat(Math.max(1, tokenLength))
        );
    }

    public int getLineNumber() { return location.line(); }
    public int getCharNumber() { return location._char(); }
}
