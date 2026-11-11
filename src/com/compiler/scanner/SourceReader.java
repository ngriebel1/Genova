package com.compiler.scanner;

public class SourceReader
{
    private final String _source;   // Constant
    private int _line;              // Dynamic
    private int _char;              // Dynamic
    private int _index;             // Dynamic

    /// Represents End-Of-File character
    public static char EOF = '\0';

    /**
     * Functions similarly to a character stream—providing methods like {@link #get()},
     * {@link #peek()}, and {@link isAtEnd()}—but operates directly over an in-memory
     * source string rather than an underlying {@link java.io.InputStream}.
     *
     * @author Nicholas J. Griebel
     */
    public SourceReader(final String source)
    {
        _source = source;
        _line = 1;
        _char = 0;
        _index = 0;
    }

    // --- METHODS -----------------------------

    /// {@code getLine(int)} retrieves from the source file a line specified
    ///     by parameter {@code int lineNumber}
    /// @return {@code String}
    public String getLine(int lineNumber) {

        if (lineNumber < 1) {
            System.err.println("The specified line number does not exist!");
            return null;
        }

        int currentLine = 1;
        int startIndex = 0;

        for (int i = 0; i <= _source.length(); i++) {
            boolean atEnd = (i == _source.length());

            // Check for newline or end of source
            if (atEnd || _source.charAt(i) == '\n') {
                if (currentLine == lineNumber) {
                    // Trim trailing carriage return if present (\r\n)
                    int endIndex = (i > 0 && _source.charAt(i - 1) == '\r') ? i - 1 : i;
                    return _source.substring(startIndex, endIndex);
                }
                currentLine++;
                startIndex = i + 1; // Next line starts right after the newline
            }
        }

        System.err.println("The specified line number does not exist!");
        return null;
    }
    /// {@code isAtEnd()} determines if end-of-string has been reached by comparing the
    ///     location of the index pointer to the size of the source string
    /// @return {@code boolean}
    public boolean isAtEnd()
    {
        return getCurrentIndex() >= _source.length();
    }
    /// {@code isAlpha(char)} determines if a char is an alphabetical character
    /// @return {@code boolean}
    public boolean isAlpha(char ch)
    {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }
    /// {@code isDigit(char)} determines if a char is a number
    /// @return {@code boolean}
    public boolean isDigit(char ch) { return (ch >= '0' && ch <= '9'); }
    /// {@code isAlphaNum(char)} determines if a char is an alphabetical OR a digit
    /// @return {@code boolean}
    public boolean isAlphaNum(char ch) { return (isAlpha(ch) || isDigit(ch)); }
    /// {@code isSpace(char)} determines if a char is a space
    /// @return {@code boolean}
    public boolean isSpace(char ch) { return (ch == ' '); }
    /// {@code isNewline(char)} determines if a char is a newline
    /// @return {@code boolean}
    public boolean isNewline(char ch) { return (ch == '\n'); }
    /// {@code isEOF(char)} determines if a char is an end-of-file symbol
    /// @return {@code boolean}
    public boolean isEOF(char ch) { return (ch == EOF); }

    /// {@code munch()} wastes a char using {@code get()}
    public void munch()
    {
        if (isAtEnd())
        {
            return;
        }

        // Munch a char
        this.get();
    }

    /// {@code peek()} returns the character currently being pointed at
    ///     but does not advance the index pointer.
    /// @return {@code char}
    public char peek()
    {
        // Note: _index always points at the "next" character
        //          due to how we handle increments in .get()
        return _source.charAt(getCurrentIndex());
    }

    /// {@code get()} returns the character currently being pointed at
    ///     and advances the index pointer by one.
    /// @return {@code char}
    public char get()
    {
        // Get the char we're pointing at
        char c = _source.charAt(getCurrentIndex());

        // Handle end-of-lines properly by using '\n' chars to increment our
        // _line and _char locators
        if (c == '\n')
        {
            ++_line;
            _char = 0;
        }
        else
        {
            ++_char;
        }

        // Move the position pointer forwards, "getting" the previous char
        ++_index;

        return c;
    }

    // --- GETTERS -----------------------------
    public int getCurrentLine()     { return _line;  }
    public int getCurrentChar()     { return _char;  }
    public int getCurrentIndex()    { return _index; }
}
