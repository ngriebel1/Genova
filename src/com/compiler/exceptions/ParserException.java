package com.compiler.exceptions;

import com.compiler.scanner.SourceReader;
import com.compiler.scanner.Token;
import org.jetbrains.annotations.NotNull;

public class ParserException extends RuntimeException {
    private final Token.Location location;
    private final int tokenLength;

    public ParserException(@NotNull String message, @NotNull Token token, int tokenLength)
    {
        this(message, token.at(), tokenLength);
    }

    public ParserException(@NotNull String message, @NotNull Token.Location location, int tokenLength)
    {
        super(message + " -> [Line " + location.line() + ", Char " + location._char() + "]");
        this.location = location;
        this.tokenLength = tokenLength;
    }

    public ParserException(@NotNull String message)
    {
        this(message, new Token.Location(0, 0), 0);
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
