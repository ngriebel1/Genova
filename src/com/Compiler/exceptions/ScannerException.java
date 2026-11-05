package com.compiler.exceptions;

import com.compiler.scanner.Token;

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
public class ScannerException extends CompilerException
{
    public ScannerException(String message, Token.Location location, int tokenLength)
    {
        super(message, location, tokenLength);
    }
}
