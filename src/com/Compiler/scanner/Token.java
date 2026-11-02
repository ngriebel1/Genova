package com.compiler.scanner;

import org.jetbrains.annotations.NotNull;

public record Token(TokenType type,
                    String lexeme,
                    Object literal,
                    Location at)
{
    /**
     * @author Nicholas Griebel
     * @param line represents the current line that this token resides on
     * @param _char represents the starting character location of this token (For
     *              compiler error displays/underlines)
     */
    public record Location(int line, int _char) {
        @Override
        public String toString() {
            return "line " + line + ", col " + _char;
        }
    }

    public static Token complex(TokenType type, String lexeme, Object literal, Location at) {
        return new Token(type, lexeme, literal, at);
    }
    public static Token simple(TokenType type, String lexeme, Location at) {
        return new Token(type, lexeme, (Object)null, at);
    }

    @Override
    public @NotNull String toString()
    {
        return "TokenType: %s -> Token Lexeme: %s -> %s".formatted(
                this.type.toString(), this.lexeme, this.at.toString()
        );
    }

    /**
     * Represents all possible tokens that the scanner will encounter
     *  during it's scanning process.
     */
    public enum TokenType
    {
        // --- Control & Special ---
        EOF, INVALID, ERROR,
        EOL,             // Keep if newlines matter (optional semicolons); otherwise safely ignore as whitespace

        // --- Keywords ---
        IF, ELSE, WHILE, FOR, DO,
        RETURN, BREAK, CONTINUE, FN,
        AUTO,            // Type inference (like modern C++)
        INCLUDE, TRUE, FALSE,
        NULL,            // Represents the keyword value 'null'

        // --- Arithmetic Operators ---
        PLUS,            // +
        MINUS,           // -
        STAR,            // *
        SLASH,           // /
        PERCENT,         // %
        PLUS_PLUS,        // ++
        MINUS_MINUS,      // --

        // --- Assignment Operators ---
        EQUAL,           // =
        PLUS_EQUAL,       // +=
        MINUS_EQUAL,      // -=
        STAR_EQUAL,       // *=
        SLASH_EQUAL,      // /=
        PERCENT_EQUAL,    // %=
        AMPERSAND_EQUAL,  // &=
        BAR_EQUAL,        // |=
        CARET_EQUAL,      // ^=
        LESS_LESS_EQUAL,  // <<=
        GREATER_GREATER_EQUAL, // >>=

        // --- Comparison Operators ---
        EQUAL_EQUAL,      // ==
        NOT_EQUAL,        // !=
        LESS,            // <
        GREATER,         // >
        LESS_EQUAL,       // <=
        GREATER_EQUAL,    // >=

        // --- Logical Operators ---
        NOT,             // !
        AND,             // &&
        OR,              // ||

        // --- Bitwise Operators ---
        AMPERSAND,       // &
        BAR,             // |
        CARET,           // ^
        TILDE,           // ~
        LESS_LESS,        // <<
        GREATER_GREATER,  // >>

        // --- Delimiters & Punctuation ---
        SEMICOLON,       // ;
        COMMA,           // ,
        DOT,             // . (Member access or method invocation)
        COLON,           // :
        DOUBLE_COLON,     // :: (Scope resolution)
        ARROW_DASH,       // -> (Commonly used for return type declarations)
        ARROW_EQUAL,      // => (Commonly used for lambdas or match expressions)
        QUESTION,        // ?

        // --- Grouping Symbols ---
        LEFT_PAREN,       // (
        RIGHT_PAREN,      // )
        LEFT_BRACE,       // {
        RIGHT_BRACE,      // }
        LEFT_BRACKET,     // [
        RIGHT_BRACKET,    // ]

        // --- Types ---
        INT, FLOAT, DOUBLE, STRING, CHAR, BOOL, VOID,
        STRUCT, ENUM, CLASS, CONST,

        // --- Literals & Dynamic Identifiers ---
        IDENTIFIER,
        INTEGER_LITERAL,
        FLOAT_LITERAL,
        STRING_LITERAL,
        CHAR_LITERAL
    }
}
