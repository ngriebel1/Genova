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
     * Reads characters and tracks source locations in an in-memory string.
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

    /// Returns the requested one-based line from the source string.
    public String getLine(int lineNumber) {

        if (lineNumber < 1) {
            System.err.println("The specified line number does not exist!");
            return null;
        }

        int currentLine = 1;
        int startIndex = 0;

        for (int i = 0; i <= _source.length(); i++) {
            boolean atEnd = (i == _source.length());

            if (atEnd || _source.charAt(i) == '\n') {
                if (currentLine == lineNumber) {
                    int endIndex = (i > 0 && _source.charAt(i - 1) == '\r') ? i - 1 : i;
                    return _source.substring(startIndex, endIndex);
                }
                currentLine++;
                startIndex = i + 1;
            }
        }

        System.err.println("The specified line number does not exist!");
        return null;
    }
    /// Returns whether the source string has been fully consumed.
    public boolean isAtEnd()
    {
        return getCurrentIndex() >= _source.length();
    }
    /// Returns whether the character is an ASCII letter.
    public boolean isAlpha(char ch)
    {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }
    /// Returns whether the character is an ASCII digit.
    public boolean isDigit(char ch) { return (ch >= '0' && ch <= '9'); }
    /// Returns whether the character is an ASCII letter or digit.
    public boolean isAlphaNum(char ch) { return (isAlpha(ch) || isDigit(ch)); }
    /// Returns whether the character is a space.
    public boolean isSpace(char ch) { return (ch == ' '); }
    /// Returns whether the character is a newline.
    public boolean isNewline(char ch) { return (ch == '\n'); }
    /// Returns whether the character is the end-of-input sentinel.
    public boolean isEOF(char ch) { return (ch == EOF); }

    /// Consumes one character if input remains.
    public void munch()
    {
        if (isAtEnd())
        {
            return;
        }

        this.get();
    }

    /// Returns the next character without advancing.
    public char peek()
    {
        return _source.charAt(getCurrentIndex());
    }

    /// Returns the next character and advances the reader.
    public char get()
    {
        char c = _source.charAt(getCurrentIndex());

        if (c == '\n')
        {
            ++_line;
            _char = 0;
        }
        else
        {
            ++_char;
        }

        ++_index;

        return c;
    }

    // --- GETTERS -----------------------------
    public int getCurrentLine()     { return _line;  }
    public int getCurrentChar()     { return _char;  }
    public int getCurrentIndex()    { return _index; }
}
