package com.compiler.exceptions;

import com.compiler.scanner.SourceReader;
import com.compiler.scanner.Token;
import org.jetbrains.annotations.NotNull;

public class ParserException extends RuntimeException {
    private final Token.Location location;
    private final int tokenLength;
    private final Token.Location endLocation;

    public ParserException(@NotNull String message, @NotNull Token token, int tokenLength)
    {
        this(message, token.at(), tokenLength);
    }

    public ParserException(@NotNull String message, @NotNull Token.Location location, int tokenLength)
    {
        super(message + " -> [Line " + location.line() + ", Char " + location._char() + "]");
        this.location = location;
        this.tokenLength = tokenLength;
        this.endLocation = null;
    }

    public ParserException(@NotNull String message, @NotNull Token start, @NotNull Token end)
    {
        this(
                message,
                start.at(),
                new Token.Location(end.at().line(), end.at()._char() + end.lexeme().length())
        );
    }

    private ParserException(
            @NotNull String message,
            @NotNull Token.Location location,
            @NotNull Token.Location endLocation
    )
    {
        super(message + " -> [Line " + location.line() + ", Char " + location._char() + "]");
        this.location = location;
        this.tokenLength = 0;
        this.endLocation = endLocation;
    }

    public ParserException(@NotNull String message)
    {
        this(message, new Token.Location(0, 0), 0);
    }

    public void printDiagnostic(SourceReader reader)
    {
        if (endLocation == null)
        {
            System.err.println(reader.getLine(location.line()));
            System.err.println(
                      " ".repeat(Math.max(0, location._char() - 1))
                    + "^".repeat(Math.max(1, tokenLength))
            );
            return;
        }

        for (int lineNumber = location.line(); lineNumber <= endLocation.line(); lineNumber++)
        {
            String line = reader.getLine(lineNumber);
            int startChar = lineNumber == location.line() ? location._char() : 1;
            int endChar = lineNumber == endLocation.line()
                    ? endLocation._char()
                    : line.length() + 1;

            System.err.println(line);
            System.err.println(
                      " ".repeat(Math.max(0, startChar - 1))
                    + "^".repeat(Math.max(1, endChar - startChar))
            );
        }
    }

    public int getLineNumber() { return location.line(); }
    public int getCharNumber() { return location._char(); }
}
