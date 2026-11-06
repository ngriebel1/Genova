package com.compiler.parser;

import com.compiler.exceptions.ParserException;
import com.compiler.scanner.Token;
import java.util.ArrayList;

/**
 * {@code TokenStream} provides a wrapper around an ArrayList<> that contains helper
 *  methods that will be used by the parser to traverse the tokenList.
 * @author Nicholas J. Griebel
 *
 * @see Token
 * @see ArrayList
 */
public class TokenStream
{
    private final ArrayList<Token> tokenList;
    private int current;

    public TokenStream(ArrayList<Token> tokenList)
    {
        this.tokenList = tokenList;
        this.current = 0;
    }

    // ----------------------------
    //  METHODS
    // ----------------------------


    public final boolean isAtEnd() {
        return (peek().type() == Token.TokenType.EOF);
    }

    /// TODO: Write a Javadoc for {@code match(Token.TokenType ... types)}
    public final boolean match(Token.TokenType ... types)
    {
        // Iterate through the included type parameters
        for (var type : types)
        {
            if (peek().type() == type)
            {
                // Consume the token
                get();

                // We got a match
                return true;
            }
        }

        // Fallthrough -- We didn't get a match.
        // No match -- No dinner
        return false;
    }

    /// TODO: Write a Javadoc for {@code consume(TokenType type, String message}
    public final Token consume(Token.TokenType type, String message) throws ParserException
    {
        if (tokenList.get(current).type() == type)
        {
            return get();
        }

        throw new ParserException(message, tokenList.get(current), tokenList.get(current).lexeme().length());
    }

    /// TODO: Write a Javadoc for {@code previous()}
    public final Token previous()
    {
        // Return the previous token (No bounds check
        // necessary since it's a history retrieval)
        return tokenList.get(current - 1);
    }

    // TODO: Write a Javadoc for {@code check(type)}
    public final boolean check(Token.TokenType type)
    {
        return (peek().type() == type);
    }

    /// TODO: Write a Javadoc for {@code peek()}
    public final Token peek()
    {
        // Return the current token WITHOUT advancing the token pointer
        return tokenList.get(current);
    }

    /// TODO: Write a Javadoc for {@code get()}
    public final Token get()
    {
        // Return the current token and advance the token pointer ONLY IF
        // we're not at the end of the tokenList
        if (!isAtEnd())
        {
            current++;
        }

        return tokenList.get(current - 1);
    }
}
