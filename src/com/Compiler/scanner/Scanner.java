package com.compiler.scanner;

// Project environment imports
import com.compiler.exceptions.ScannerException;

// Lib Imports
import java.util.List;
import java.util.ArrayList;

/**
 * The {@code Scanner} class is responsible for lexical analysis
 * of the source code. It reads the raw character stream and breaks it down
 * into a sequence of tokens, which are then consumed by the parser.
 *
 * <p>It handles tasks such as identifying keywords, identifiers, literals,
 * and operators while tracking source locations for error reporting.</p>
 *
 * @since 1.0
 * @see Token
 * @see scanLine
 * @see scan
 */
public class Scanner {
    private final String source;
    private SourceReader reader;
    private final ArrayList<Token> tokenList;

    private Token.Location currentLocation;     /// currentLocation contains both currentLine & currentChar data
    private int startLocation;                  /// startLocation is the first character of a discovered token

    public Scanner(String source)
    {
        this.source = source;
        tokenList = new ArrayList<Token>();
        reader = new SourceReader(source);

        this.startLocation = 0;
        this.currentLocation = new Token.Location(0,0);

    }

    /**
     * scans a single line of input for valid tokens, identifiers, types & literals.
     * Valid tokens are deposited into an {@code ArrayList<Token>}
     * @author Nicholas Griebel
     * @return {@code List<Token>}
     * @see scan()
     * @see ArrayList
     */
    public List<Token> scanLine() throws ScannerException
    {
        while (!reader.isAtEnd())
        {
            // Get the next character from the reader
            char ch = reader.get();

            Token currentToken = switch(ch)
            {
                // *------------*-------------*-------------*
                //  MATH OPERATORS
                // *------------*-------------*-------------*

                // ADD
                case '+' -> {
                    // Yield a compound operator '+='
                    if (reader.peek() == '=')
                    {
                        // Absorb the leading equals
                        reader.get();

                        yield Token.simple(
                                Token.TokenType.PLUS_EQUAL,
                                "+=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield a single operator '+'
                    yield Token.simple(
                            Token.TokenType.PLUS,
                            "+",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // SUBTRACT
                case '-' -> {
                    // Yield a compound operator '-='
                    if (reader.peek() == '=')
                    {
                        // Absorb the leading equals
                        reader.get();

                        yield Token.simple(
                                Token.TokenType.MINUS_EQUAL,
                                "-=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield a single operator '-'
                    yield Token.simple(
                            Token.TokenType.MINUS,
                            "-",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );

                }
                // MULTIPLY
                case '*' -> {
                    // Yield a compound operator '*='
                    if (reader.peek() == '=')
                    {
                        // Absorb the leading equals
                        reader.get();

                        yield Token.simple(
                                Token.TokenType.STAR_EQUAL,
                                "*=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield a single operator '*'
                    yield Token.simple(
                            Token.TokenType.STAR,
                            "*",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // FORWARD SLASH
                case '/' -> {
                    // SINGLE-LINE COMMENT
                    if (reader.peek() == '/') {
                        // Waste all chars until we reach end of line (or end of string)
                        while (!reader.isAtEnd() && reader.peek() != '\n') {
                            // Absorb the comment character
                            reader.get();
                        }

                        yield (Token) null;
                        // While loop will expire on next loop
                    }
                    // BLOCK COMMENT
                    else if (reader.peek() == '*')
                    {
                        reader.munch();
                        while (!reader.isAtEnd())
                        {
                            // Have we reached the outer symbols yet? (i.e */ )
                            if (reader.peek() == '*')
                            {
                                // Get rid of the star
                                reader.munch();
                                if (reader.peek() == '/')
                                {
                                    // Get rid of the slash
                                    reader.munch();

                                    // And yield a null token (comment complete)
                                    yield (Token)null;
                                }
                            }
                            else
                            {
                                // Otherwise, keep munching the comment chars
                                reader.munch();
                            }
                        }
                    }

                    // SLASH-EQUALS
                    else if (reader.peek() == '=') {
                        reader.get();

                        yield Token.simple(
                                Token.TokenType.SLASH_EQUAL,
                                "/=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // SLASH
                    yield Token.simple(
                            Token.TokenType.SLASH,
                            "/",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                default ->
                {
                    yield (Token)null;
                    // TODO: Utilize newly developed isdigit/isalpha methods in SourceReader to
                    //          find reserved words/identifiers/literals.
                }
            };  // End of switch expression

            if (currentToken != (Token)null)
            {
                // Only add non-null tokens
                // (null tokens can represent comments)
                tokenList.add(currentToken);
            }
        }

        return this.tokenList;
    }

    /**
     * {@code scan()} can be used to scan an entire string of input for valid
     * tokens. This method will scan until it reaches an <b>EOF</b>, depositing
     * valid tokens into a token list.
     * @return {@code List<Token>}
     * @see Token
     */
    public List<Token> scan() throws ScannerException
    {
        // TEMPORARY -- REMOVE AS SOON AS POSSIBLE
        return new ArrayList<Token>();
    }

    /// The {@code getCurrentLine()} getter has been included to provide
    ///     line data to the compilers error handling system. It allows
    ///     extensive error reporting to be implemented
    public int getCurrentLine() { return reader.getCurrentLine(); }

    /// Similar to {@code getCurrentLine()}
    /// @see getCurrentLine()
    public int getCurrentChar() { return reader.getCurrentChar(); }

    public int getCurrentIndex() { return reader.getCurrentIndex(); }
}
