package com.compiler.exceptions;

import com.compiler.scanner.Token;

public class ParserException extends CompilerException {
    public ParserException(String message, Token token, int tokenLength)
    {
        super(
                message,
                token.at(),
                tokenLength
        );
    }
}
