
package com.compiler.exceptions;

import com.compiler.scanner.Token;
import com.compiler.scanner.SourceReader;

public class CompilerException extends RuntimeException {
    private final Token.Location location;
    private final int tokenLength;

    public CompilerException(String message, Token.Location location, int tokenLength)
    {
        this.tokenLength = tokenLength;
        this.location = location;
        super(message + " -> [Line " + location.line() + ", Char " + location._char() + "]");
    }

    public void printDiagnostic(SourceReader reader)
    {
        // First, let's display the line that caused the exception
        System.err.println(reader.getLine(location.line()));
        System.err.println(
                  " ".repeat(location._char() - 2)
                + "~".repeat(tokenLength + 1)
                + "^"
        );
    }

    public int getLineNumber() { return this.location.line(); }
    public int getCharNumber() { return this.location._char(); }
}
