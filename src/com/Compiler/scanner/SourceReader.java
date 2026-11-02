package com.compiler.scanner;

// project environment imports
import com.compiler.exceptions.ScannerException;

public class SourceReader
{
    private final String _source;
    private int _line;
    private int _char;
    private int _index;

    public SourceReader(final String source)
    {
        _source = source;
        _line = 1;
        _char = 0;
        _index = 0;
    }

    // --- METHODS -----------------------------

    /// {@code isAtEnd()} determines if end-of-string has been reached by comparing the
    ///     location of the index pointer to the size of the source string
    /// @return {@code boolean}
    public boolean isAtEnd()
    {
        return getCurrentIndex() >= _source.length();
    }

    /// {@code isalpha(char)} determines if a char is an alphabetical character
    /// @return {@code boolean}
    public boolean isalpha(char ch)
    {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    /// {@code putback()} puts a character "back into the string" (reverses the index by 1)
    public void putback()
    {
        // TODO: Implement the putback() method
    }

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

        // Move the position pointer forwards, "getting" the previous char
        ++_index;
        ++_char;

        return c;
    }

    // --- GETTERS -----------------------------
    public int getCurrentLine()     { return _line;  }
    public int getCurrentChar()     { return _char;  }
    public int getCurrentIndex()    { return _index; }
}
