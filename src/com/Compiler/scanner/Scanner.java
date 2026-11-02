package com.compiler.scanner;

// Project environment imports
import com.compiler.exceptions.ScannerException;
import com.compiler.scanner.Token.TokenType;

// Lib Imports
import java.util.Map;
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
    private final SourceReader reader;

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            // Control Flow
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("while", TokenType.WHILE),
            Map.entry("for", TokenType.FOR),
            Map.entry("do", TokenType.DO),
            Map.entry("return", TokenType.RETURN),
            Map.entry("break", TokenType.BREAK),
            Map.entry("continue", TokenType.CONTINUE),

            // Functions & Classes / OOP
            Map.entry("fn", TokenType.FN),
            Map.entry("class", TokenType.CLASS),
            Map.entry("super", TokenType.SUPER),
            Map.entry("sub", TokenType.SUB),
            Map.entry("inherits", TokenType.INHERITS),
            Map.entry("object", TokenType.OBJECT),
            Map.entry("this", TokenType.THIS),

            // Primitive & Built-in Types
            Map.entry("int", TokenType.INT),
            Map.entry("float", TokenType.FLOAT),
            Map.entry("double", TokenType.DOUBLE),
            Map.entry("string", TokenType.STRING),
            Map.entry("char", TokenType.CHAR),
            Map.entry("bool", TokenType.BOOL),
            Map.entry("void", TokenType.VOID),

            // Data Structures & Modifiers
            Map.entry("struct", TokenType.STRUCT),
            Map.entry("enum", TokenType.ENUM),
            Map.entry("const", TokenType.CONST),
            Map.entry("template", TokenType.TEMPLATE),
            Map.entry("static", TokenType.STATIC),
            Map.entry("dynamic", TokenType.DYNAMIC),
            Map.entry("override", TokenType.OVERRIDE),

            // Miscellaneous & Literals
            Map.entry("auto", TokenType.AUTO),
            Map.entry("include", TokenType.INCLUDE),
            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE),
            Map.entry("null", TokenType.NULL),
            Map.entry("typeof", TokenType.TYPEOF),
            Map.entry("instanceof", TokenType.INSTANCEOF),

            // Access Modifiers
            Map.entry("public", TokenType.PUBLIC),
            Map.entry("private", TokenType.PRIVATE),
            Map.entry("protected", TokenType.PROTECTED)
    );

    public Scanner(String source)
    {
        this.source = source;
        reader = new SourceReader(source);

    }

    public Token scanNumber(char initial)
    {
        StringBuilder literal = new StringBuilder();
        int line = reader.getCurrentLine();
        int start = reader.getCurrentChar();

        // Add our initial character
        literal.append(initial);

        // Retrieve the integer part of the number
        while (!reader.isAtEnd() && (reader.isDigit(reader.peek())))
        {
            // Add the character to our StringBuilder string
            literal.append(reader.get());
        }

        // Determine if we're dealing with a floating point number
        if (!reader.isAtEnd() && reader.peek() == '.')
        {
            char ch = reader.get();
            if (reader.isDigit(reader.peek()))
            {
                // Add our decimal since we verified numbers are after it
                literal.append(ch);

                while (reader.isDigit(reader.peek()))
                {
                    literal.append(reader.get());
                }
            }
            else
            {
                throw new ScannerException(
                        "Malformed floating point literal",
                        new Token.Location(
                                line,
                                start
                        )
                );
            }
        }
        else
        {
            return Token.complex(
                    TokenType.INTEGER_LITERAL,
                    literal.toString(),
                    Integer.parseInt(literal.toString()),
                    new Token.Location(
                            line,
                            start
                    )
            );
        }

        return Token.complex(
                TokenType.FLOAT_LITERAL,
                literal.toString(),
                Double.parseDouble(literal.toString()),
                new Token.Location(
                        line,
                        start
                )
        );
    }

    public Token scanWord(char initial)
    {
        StringBuilder lexeme = new StringBuilder();
        int line = reader.getCurrentLine();
        int start = reader.getCurrentChar();

        // Add our initial character
        lexeme.append(initial);

        // Continue to collect characters so long as:
        //      1: We are not at the end of the string
        //  and 2: The character is an alphabetical, a number, or an underscore
        while (!reader.isAtEnd() && (reader.isAlphaNum(reader.peek()) || reader.peek() == '_'))
        {
            // Add the character to our StringBuilder string
            lexeme.append(reader.get());
        }

        TokenType type = KEYWORDS.getOrDefault(lexeme.toString(), TokenType.IDENTIFIER);

        return Token.simple(
                type,
                lexeme.toString(),
                new Token.Location(
                        line,
                        start
                )
        );
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
        ArrayList<Token> tokenList = new ArrayList<>();
        boolean lineEnded = false;
        while (!reader.isAtEnd() && !lineEnded)
        {
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
                                TokenType.PLUS_EQUAL,
                                "+=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield a single operator '+'
                    yield Token.simple(
                            TokenType.PLUS,
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
                                TokenType.MINUS_EQUAL,
                                "-=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield a single operator '-'
                    yield Token.simple(
                            TokenType.MINUS,
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
                                TokenType.STAR_EQUAL,
                                "*=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield a single operator '*'
                    yield Token.simple(
                            TokenType.STAR,
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
                                TokenType.SLASH_EQUAL,
                                "/=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // SLASH
                    yield Token.simple(
                            TokenType.SLASH,
                            "/",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // KEYWORDS, IDENTIFIERS, NUMBER LITERALS, EXCEPTIONS
                default ->
                {
                    if (reader.isAlpha(ch))
                    {
                        // Reserved words/Identifiers
                        yield scanWord(ch);
                    }
                    else if (reader.isDigit(ch))
                    {
                        yield scanNumber(ch);
                    }
                    else if (reader.isNewline(ch))
                    {
                        // Indicate an end-of-line was reached
                        lineEnded = true;
                        yield (Token)null;
                    }
                    else if (reader.isEOF(ch))
                    {
                        yield (Token)null;
                    }
                    else if (reader.isSpace(ch))
                    {
                        // No need to handle spaces. Yield a waste token
                        yield (Token)null;
                    }
                    else
                    {
                        // The only other option is an unexpected character or
                        // a malformed token.
                        throw new ScannerException(
                                "Unexpected character/Malformed Token at",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                }
            };  // End of switch expression

            if (currentToken != (Token)null)
            {
                // Only add non-null tokens
                // (null tokens can represent comments)
                tokenList.add(currentToken);
            }
        }

        return tokenList;
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

    public boolean isAtEnd() { return reader.isAtEnd(); }

    /// The {@code getCurrentLine()} getter has been included to provide
    ///     line data to the compilers error handling system. It allows
    ///     extensive error reporting to be implemented
    public int getCurrentLine() { return reader.getCurrentLine(); }

    /// Similar to {@code getCurrentLine()}
    /// @see getCurrentLine()
    public int getCurrentChar() { return reader.getCurrentChar(); }

    public int getCurrentIndex() { return reader.getCurrentIndex(); }
}
