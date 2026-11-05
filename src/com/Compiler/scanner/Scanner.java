/// Written by Nicholas J. Griebel
/// Development began 11/1/2026
///
/// The Scanner class contains a lexical analyzer for the Genova interpreted programming language. <br>
/// A source string is used to construct a new Scanner instance, and either {@code scanLine()} or <br>
/// {@code scan()} is used to retrieve all valid tokens from the source string. In the event that <br>
/// invalid or malformed tokens are discovered, a ScannerException is thrown indicating what went <br>
/// wrong and where.

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
 * @author Nicholas J. Griebel
 * @since 1.0
 * @see Token
 * @see SourceReader
 * @see scanLine()
 * @see scan()
 */
public class Scanner {
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
        // The only thing we need to do is initialize our SourceReader
        reader = new SourceReader(source);
    }

    // TODO: Write Javadoc for scanString
    private Token scanString()
    {
        StringBuilder stringLiteral = new StringBuilder();

        // Get some initial information for exception handling
        int startCharLocation = reader.getCurrentChar();
        int startLineLocation = reader.getCurrentLine();

        // Initial char placed into ch placeholder to perform
        // blank string check (If ch is the ending quotes, we have
        // a blank string. No information needs to be scanned)
        char ch = reader.get();
        if (ch == '"')
        {
            return Token.complex(
                    TokenType.STRING_LITERAL,
                    "",
                    "",
                    new Token.Location(
                            reader.getCurrentLine(),
                            reader.getCurrentChar()
                    )
            );
        }

        // Append our first character
        stringLiteral.append(ch);

        while (!reader.isAtEnd() && reader.peek() != '"')
        {
            int locationBeforeNewline = reader.getCurrentChar();
            char c = reader.get();

            if (c == '\n')
            {
                Token.Location location = new Token.Location(
                        reader.getCurrentLine() - 1,
                        startCharLocation
                );

                throw new ScannerException(
                        "Unterminated string literal at ",
                        new Token.Location(
                                reader.getCurrentLine() - 1,
                                startCharLocation
                        ),
                        locationBeforeNewline - startCharLocation
                );
            }

            if (c == '\\')
            {
                if (reader.isAtEnd())
                {
                    throw new ScannerException(
                            "Unterminated Escape Sequence",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    startCharLocation
                            ),
                            // Length of the token
                            locationBeforeNewline - startCharLocation
                    );
                }

                char escape = reader.get();
                c = switch(escape)
                {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    case '\'' -> '\'';
                    default -> throw new ScannerException(
                            "Invalid Escape Sequence \\" + escape,
                            new Token.Location(startLineLocation, startCharLocation),
                            // Get length of token
                            reader.getCurrentChar() - startCharLocation
                    );
                };
            }

            stringLiteral.append(c);
        }

        if (reader.isAtEnd())
        {
            throw new ScannerException(
                    "Unterminated String Literal at ",
                    new Token.Location(
                            startLineLocation,
                            startCharLocation
                    ),

                    reader.getCurrentChar() - startCharLocation
            );
        }

        // Get rid of the closing double quotes
        reader.get();

        return Token.complex(
                TokenType.STRING_LITERAL,
                stringLiteral.toString(),
                stringLiteral.toString(),
                new Token.Location(
                        startLineLocation,
                        startCharLocation
                )
        );
    }

    // TODO: Write Javadoc for scanChar
    private Token scanChar()
    {
        char literal;
        int startCharLocation = reader.getCurrentChar();
        int startLineLocation = reader.getCurrentLine();

        // Get our char
        if (!reader.isAtEnd() || reader.peek() != '\n')
        {
            literal = reader.get();
        }
        else
        {
            throw new ScannerException(
                    "Unterminated char literal at ",
                    new Token.Location(
                            startLineLocation,
                            startCharLocation
                    ),
                    // Get length of token
                    reader.getCurrentChar() - startCharLocation
            );
        }

        if (!reader.isAtEnd() && reader.peek() == '\'')
        {
            // Get rid of the trailing single quote
            reader.get();

            // Return the char_literal token
            return Token.complex(
                    TokenType.CHAR_LITERAL,
                    "" + literal,
                    literal,
                    new Token.Location(
                            reader.getCurrentLine(),
                            reader.getCurrentChar()
                    )
            );
        }

        // Throw a ScannerException error for the unterminated char literal
        throw new ScannerException(
                "Unterminated char literal at ",
                new Token.Location(
                        reader.getCurrentLine(),
                        reader.getCurrentChar()
                ),
                // Get length of token
                reader.getCurrentChar() - startCharLocation
        );
    }

    // TODO: Write Javadoc for scanNumber
    // WARNING: scanNumber's FLOAT scanning is failing JUnit test.
    //          Discover why as soon as possible!
    private Token scanNumber(char initial)
    {
        StringBuilder literal = new StringBuilder();
        int startLineLocation = reader.getCurrentLine();
        int startCharLocation = reader.getCurrentChar();

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
            if (!reader.isAtEnd() && reader.isDigit(reader.peek()))
            {
                // Add our decimal since we verified numbers are after it
                literal.append(ch);

                while (!reader.isAtEnd() && reader.isDigit(reader.peek()))
                {
                    literal.append(reader.get());
                }
            }
            else
            {
                throw new ScannerException(
                        "Malformed floating point literal",
                        new Token.Location(
                                startLineLocation,
                                startCharLocation
                        ),
                        // Get token length
                        reader.getCurrentChar() - startCharLocation
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
                            startLineLocation,
                            startCharLocation
                    )
            );
        }

        return Token.complex(
                TokenType.FLOAT_LITERAL,
                literal.toString(),
                Double.parseDouble(literal.toString()),
                new Token.Location(
                        startLineLocation,
                        startCharLocation
                )
        );
    }

    // TODO: Write Javadoc for scanWord
    private Token scanWord(char initial)
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

        // boolean lineEnded is a flag used to indicate whether we've reached
        //     the end of a line (where a line ends on '\n')
        boolean lineEnded = false;

        while (!reader.isAtEnd() && !lineEnded)
        {
            char ch = reader.get();

            // Determine which token we are looking at
            Token currentToken = switch(ch)
            {
                // Note: due to the nature of token processing, some symbols are mixed into other blocks.
                // Check the header comment in each block to see if the one you need is in it (check the
                //       first character and start at that block )

                // *------------*-------------*-------------*
                //  MATH OPERATORS
                // *------------*-------------*-------------*

                // ADD
                case '+' -> {
                    // Yield a compound operator '+='
                    if (!reader.isAtEnd() && reader.peek() == '=')
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
                    // Yield a compound operator '++'
                    else if (!reader.isAtEnd() && reader.peek() == '+')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.PLUS_PLUS,
                                "++",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Else yield a single operator '+'
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
                    // Yield a compound operator '->'
                    if (!reader.isAtEnd() && reader.peek() == '>')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.ARROW_DASH,
                                "->",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // Yield a compound operator '-='
                    else if (!reader.isAtEnd() && reader.peek() == '=')
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
                    // Yield a compound operator '--'
                    else if (!reader.isAtEnd() && reader.peek() == '-')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.MINUS_MINUS,
                                "--",
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
                    if (!reader.isAtEnd() && reader.peek() == '=')
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
                    if (!reader.isAtEnd() && reader.peek() == '/') {
                        // Waste all chars until we reach end of line (or end of string)
                        while (!reader.isAtEnd() && reader.peek() != '\n') {
                            // Absorb the comment character
                            reader.get();
                        }

                        yield (Token) null;
                        // While loop will expire on next loop
                    }
                    // BLOCK COMMENT
                    else if (!reader.isAtEnd() && reader.peek() == '*')
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
                    else if (!reader.isAtEnd() && reader.peek() == '=') {
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
                // PERCENT
                case '%' -> {
                    // Yield compound operator token "%="
                    if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.PERCENT_EQUAL,
                                "%=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield single token '%'
                    yield Token.simple(
                            TokenType.PERCENT,
                            "%",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // EQUALS
                case '=' -> {
                    // ARROW_EQUAL
                    if (!reader.isAtEnd() && reader.peek() == '>')
                    {
                        reader.get();

                        // Yield ARROW_EQUAL
                        yield Token.simple(
                                TokenType.ARROW_EQUAL,
                                "=>",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // EQUAL_EQUAL
                    else if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        // Absorb trailing equals
                        reader.get();

                        // Yield EQUAL_EQUAL
                        yield Token.simple(
                                TokenType.EQUAL_EQUAL,
                                "==",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // No trailing equal, yield single EQUAL token.
                    yield Token.simple(
                            TokenType.EQUAL,
                            "=",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }

                // *------------*-------------*-------------*
                //  LOGICAL / BITWISE / COMPARISON OPERATORS
                // *------------*-------------*-------------*

                // NOT
                case '!' -> {
                    // Yield a NOT EQUALS token '!='
                    if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.NOT_EQUAL,
                                "!=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield NOT token
                    yield Token.simple(
                            TokenType.NOT,
                            "!",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // AMPERSAND
                case '&' -> {
                    // Yield compound token AND ('&&')
                    if (!reader.isAtEnd() && reader.peek() == '&')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.AND,
                                "&&",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // Yield compound token '&='
                    else if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.AMPERSAND_EQUAL,
                                "&=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield token AMPERSAND '&'
                    yield Token.simple(
                            TokenType.AMPERSAND,
                            "&",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // BAR
                case '|' -> {
                    if (!reader.isAtEnd() && reader.peek() == '|')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.OR,
                                "||",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    else if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.BAR_EQUAL,
                                "|=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    yield Token.simple(
                            TokenType.BAR,
                            "|",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // CARET
                case '^' -> {
                    // Yield compound operator CARET_EQUAL ('^=')
                    if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.CARET_EQUAL,
                                "^=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield single operator CARET ('^')
                    yield Token.simple(
                            TokenType.CARET,
                            "^",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // TILDE
                case '~' -> Token.simple(TokenType.TILDE,"~",new Token.Location(reader.getCurrentLine(), reader.getCurrentChar()));
                // LESS
                case '<' -> {
                    // LESS_LESS
                    if (!reader.isAtEnd() && reader.peek() == '<')
                    {
                        reader.get();

                        // LESS_LESS_EQUAL '<<='
                        if (!reader.isAtEnd() && reader.peek() == '=')
                        {
                            reader.get();

                            yield Token.simple(
                                    TokenType.LESS_LESS_EQUAL,
                                    "<<=",
                                    new Token.Location(
                                            reader.getCurrentLine(),
                                            reader.getCurrentChar()
                                    )
                            );
                        }

                        // Yield LESS_LESS
                        yield Token.simple(
                                TokenType.LESS_LESS,
                                "<<",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // LESS_EQUAL
                    else if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        // Yield LESS_EQUAL
                        yield Token.simple(
                                TokenType.LESS_EQUAL,
                                "<=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    // Yield LESS
                    yield Token.simple(
                            TokenType.LESS,
                            "<",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }
                // GREATER
                case '>' -> {
                    // GREATER_GREATER
                    if (!reader.isAtEnd() && reader.peek() == '>')
                    {
                        reader.get();

                        // GREATER_GREATER_EQUAL
                        if (!reader.isAtEnd() && reader.peek() == '=')
                        {
                            reader.get();

                            yield Token.simple(
                                    TokenType.GREATER_GREATER_EQUAL,
                                    ">>=",
                                    new Token.Location(
                                            reader.getCurrentLine(),
                                            reader.getCurrentChar()
                                    )
                            );
                        }

                        yield Token.simple(
                                TokenType.GREATER_GREATER,
                                ">>",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }
                    // GREATER_EQUAL
                    else if (!reader.isAtEnd() && reader.peek() == '=')
                    {
                        reader.get();

                        yield Token.simple(
                                TokenType.GREATER_EQUAL,
                                ">=",
                                new Token.Location(
                                        reader.getCurrentLine(),
                                        reader.getCurrentChar()
                                )
                        );
                    }

                    yield Token.simple(
                            TokenType.GREATER,
                            ">",
                            new Token.Location(
                                    reader.getCurrentLine(),
                                    reader.getCurrentChar()
                            )
                    );
                }

                // *------------*-------------*-------------*
                //  DELIMITERS / PUNCTUATIONS
                // *------------*-------------*-------------*

                // DOUBLE QUOTE (STRING)
                case '"' -> scanString();
                // SINGLE QUOTE (CHAR)
                case '\'' -> scanChar();
                // COMMA
                case ',' -> Token.simple(TokenType.COMMA,",",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // DOT
                case '.' -> Token.simple(TokenType.DOT,".",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // COLON
                case ':' -> Token.simple(TokenType.COLON,":",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // QUESTION MARK
                case '?' -> Token.simple(TokenType.QUESTION,"?",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // SEMICOLON
                case ';' -> Token.simple(TokenType.SEMICOLON, ";", new Token.Location(reader.getCurrentLine(), reader.getCurrentChar()));

                // *------------*-------------*-------------*
                //  GROUPING SYMBOLS
                // *------------*-------------*-------------*

                // LEFT PAREN
                case '(' -> Token.simple(TokenType.LEFT_PAREN,"(",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // RIGHT PAREN
                case ')' -> Token.simple(TokenType.RIGHT_PAREN,")",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // LEFT BRACE
                case '{' -> Token.simple(TokenType.LEFT_BRACE,"{",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // RIGHT BRACE
                case '}' -> Token.simple(TokenType.RIGHT_BRACE,"}",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));
                // LEFT BRACKET
                case '[' -> Token.simple(TokenType.LEFT_BRACKET,"[",new Token.Location(reader.getCurrentLine(), reader.getCurrentChar()));
                // RIGHT BRACKET
                case ']' -> Token.simple(TokenType.RIGHT_BRACKET,"]",new Token.Location(reader.getCurrentLine(),reader.getCurrentChar()));

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
                        lineEnded = true;
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
                                ),

                                1 // length of malformed token/character
                        );
                    }
                }
            };  // End of switch expression

            if (currentToken != (Token)null)
            {
                // Only add non-null tokens
                // (null tokens can represent comments, spaces, etc)
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

    /// Return a copy of the sourceReader for proper printing of
    /// lines containing exceptions.
    /// @return {@code SourceReader}
    public SourceReader getReader() { return this.reader; }
}
