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
        public @NotNull String toString() {
            return "line " + line + ", col " + _char;
        }
    }

    /**
     * Generates a token that contains an object literal
     * @param type The type of token being generated. See {@link Token.TokenType}
     * @param lexeme The raw text in {@code String} form
     * @param literal The Object literal (For example, if this is an INT_LITERAL token, this
     *                parameter should be an {@code int} object)
     * @param at The location where this token resides in the source string. See {@link Token.Location}
     * @return The generated {@link Token}
     * @author Nicholas J. Griebel
     */
    public static Token complex(TokenType type, String lexeme, Object literal, Location at)
    {
        return new Token(type, lexeme, literal, at);
    }

    /**
     * Generates a token that does not contain an object literal
     * @param type The type of token being generated. See {@link Token.TokenType}
     * @param lexeme The raw text in {@code String} form
     * @param at The location where this token resides in the source string. See {@link Token.Location}
     * @return The generated {@link Token}
     * @author Nicholas J. Griebel
     */
    public static Token simple(TokenType type, String lexeme, Location at)
    {
        return new Token(type, lexeme, (Object)null, at);
    }

    /**
     * Returns a string representation of the currently instantiated {@link Token}.
     *  The representation contains the {@link TokenType}, {@code Lexeme}, and it's
     *  {@link Token.Location} in the source string.
     * @return A string representation of this {@link Token}
     * @author Nicholas J. Griebel
     * @implNote This method overrides {@link Object}'s generic {@code toString()} method.
     */
    @Override
    public @NotNull String toString()
    {
        return "TokenType: %s -> Token Lexeme: %s -> %s".formatted(
                this.type.toString(), this.lexeme, this.at.toString()
        );
    }

    /**
     * Getter for the lexeme attribute
     * @return A {@code String} containing the raw textual value of the currently instantiated token
     * @author Nicholas J. Griebel
     */
    public String getLexeme()
    {
        return lexeme;
    }

    /**
     * Represents all possible tokens that the scanner will encounter
     *  during it's scanning process.
     */
    public enum TokenType
    {
        // --- Control & Special ---
        EOF, INVALID, ERROR, EOL,

        // --- Keywords ---
        IF, ELSE, WHILE, FOR, DO,
        RETURN, BREAK, CONTINUE, FN,
        CLASS, SUPER, SUB, INHERITS,
        CONST, TEMPLATE, STATIC,
        DYNAMIC, OVERRIDE,
        AUTO, INCLUDE, TRUE, FALSE, NULL,
        TYPEOF, INSTANCEOF, THIS,
        PUBLIC, PRIVATE, PROTECTED,

        // --- Type names ---
        OBJECT, INT, FLOAT, DOUBLE,
        STRING, CHAR, BOOL, VOID, STRUCT,
        ENUM,


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

        // --- Literals & Dynamic Identifiers ---
        IDENTIFIER,
        INTEGER_LITERAL,
        FLOAT_LITERAL,
        STRING_LITERAL,
        CHAR_LITERAL
    }
}
