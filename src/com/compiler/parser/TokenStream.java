package com.compiler.parser;

import com.compiler.exceptions.ParserException;
import com.compiler.scanner.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * {@code TokenStream} provides a wrapper around an ArrayList<> that contains helper
 *  methods that will be used by the parser to traverse a tokenList.
 * @author Nicholas J. Griebel
 *
 * @see Token
 * @see ArrayList
 */
public class TokenStream
{
    // --------------------------------------------
    //  ATTRIBUTES
    // --------------------------------------------
    private final ArrayList<Token> tokenList;
    private int current;


    // --------------------------------------------
    //  CONSTRUCTORS
    // --------------------------------------------

    /**
     * Primary constructor for the {@link TokenStream} class
     * @param tokenList of type {@code ArrayList<Token>} containing tokens
     *   retrieved during the scanning process
     */
    public TokenStream(@NotNull ArrayList<Token> tokenList) throws ParserException
    {
        this.tokenList = tokenList;
        this.current = 0;
    }

    // --------------------------------------------
    //  METHODS
    // --------------------------------------------

    /**
     * Checks if the token stream has reached the end by inspecting
     * whether the current lookahead token is of type {@link Token.TokenType#EOF}.
     *
     * @return {@code true} if the current token is EOF, {@code false} otherwise
     * @author Nicholas J. Griebel
     */
    public final boolean isAtEnd()
    {
        return (peek().type() == Token.TokenType.EOF);
    }

    /**
     * Checks if the current lookahead token matches any of the specified token types.
     * If a match is found, the token is automatically consumed and {@code true} is returned;
     * otherwise, the stream remains unchanged and {@code false} is returned.
     *
     * @param types the token types to check against the current lookahead
     * @return {@code true} if a matching token was found and consumed, {@code false} otherwise
     * @author Nicholas J. Griebel
     */
    public final boolean match(Token.TokenType @NotNull ... types)
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

    /**
     * Consumes the current token if its type matches the expected type;
     * otherwise, throws a {@link ParserException} with the provided error message.
     *
     * @param type    the expected token type to match and consume
     * @param message the error message to throw if the token type does not match
     * @return the consumed {@link Token}
     * @throws ParserException if the current token does not match the expected type
     * @author Nicholas J. Griebel
     */
    public final Token consume(Token.TokenType type, String message) throws ParserException
    {
        if (tokenList.get(current).type() == type)
        {
            return get();
        }

        Token unexpected = tokenList.get(current);
        String found = unexpected.type() == Token.TokenType.EOF
                ? "end of input"
                : "'" + unexpected.lexeme() + "'";
        throw new ParserException(
                message + ", found " + found,
                unexpected,
                unexpected.lexeme().length()
        );
    }

    /**
     * Retrieves the previously consumed token from the token stream history.
     *
     * @return the previous {@link Token} in the sequence
     * @author Nicholas J. Griebel
     */
    public final Token previous()
    {
        // Return the previous token (No bounds check
        // necessary since it's a history retrieval)
        return tokenList.get(current - 1);
    }

    /**
     * Checks if the current lookahead token matches the specified token type
     * without consuming or advancing the token stream.
     *
     * @param type the token type to check against the current lookahead
     * @return {@code true} if the current token matches the given type, {@code false} otherwise
     * @author Nicholas J. Griebel
     */
    public final boolean check(Token.TokenType type)
    {
        return (peek().type() == type);
    }

    /**
     * Retrieves the current lookahead token from the token stream without advancing
     * the internal token pointer.
     *
     * @return the current {@link Token}
     * @author Nicholas J. Griebel
     */
    public final Token peek()
    {
        // Return the current token WITHOUT advancing the token pointer
        return tokenList.get(current);
    }

    /**
     * Retrieves the current token and advances the internal token pointer
     * to the next position, provided the end of the token stream has not been reached.
     *
     * @return the current {@link Token} before advancing, or the final token if at the end
     * @author Nicholas J. Griebel
     */
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
