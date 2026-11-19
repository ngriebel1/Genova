package com.compiler.parser;

import com.compiler.ast.Expr;
import com.compiler.exceptions.ParserException;
import com.compiler.scanner.Token;
import com.compiler.parser.TokenStream;

public class ExpressionParser {
    private final TokenStream tokenStream;

    public ExpressionParser(final TokenStream tokenStream)
    {
        this.tokenStream = tokenStream;
    }

    /**
     * Parses an expression.
     *
     * @return the parsed expression
     */
    public Expr parseExpression() {
        return assignment();
    }

    /**
     * Assignment   -> Unary Assignop Assignment | Equality
     * Assignop     -> "=" | "+=" | "-=" | "*=" | "/=" | "%="
     *
     * @return the parsed assignment or non-assignment expression
     */
    private Expr assignment() {
        Token expressionStart = tokenStream.peek();

        Expr expression = equality();
        Token expressionEnd = tokenStream.previous();

        // Look for our assignment operator
        if (tokenStream.match(
                Token.TokenType.EQUAL, Token.TokenType.PLUS_EQUAL,
                Token.TokenType.MINUS_EQUAL, Token.TokenType.STAR_EQUAL,
                Token.TokenType.SLASH_EQUAL, Token.TokenType.PERCENT_EQUAL
        )
        ) {
            Token assignop = tokenStream.previous();

            // Check if we're a unary that uses '*'
            boolean isDereference = expression instanceof Expr.Unary unary
                    && unary.Unaryop().type() == Token.TokenType.STAR;

            // Throw an error if we're not a variable, or we are a unary that does NOT use '*'
            if (!(expression instanceof Expr.Variable) && !isDereference) {
                throw new ParserException("Invalid assignment syntax at", expressionStart, expressionEnd);
            }

            return new Expr.Assign(expression, assignop, assignment());
        }

        return expression;
    }

    /**
     * Equality     -> Relational (Equalityop Relational)*
     * Equalityop   -> "==" | "!="
     *
     * @return the parsed equality expression
     */
    private Expr equality() {
        Expr expression = relational();

        while (tokenStream.match(Token.TokenType.EQUAL_EQUAL, Token.TokenType.NOT_EQUAL)) {
            Token operator = tokenStream.previous();
            Expr right = relational();

            expression = new Expr.Binary(
                    expression,
                    operator,
                    right
            );
        }

        return expression;
    }

    /**
     * Relational       -> Addition (Relationalop Addition)*
     * Relationalop     -> ">" | ">=" | "<" | "<="
     *
     * @return the parsed relational expression
     */
    private Expr relational() {
        Expr expression = addition();

        while (tokenStream.match(
                Token.TokenType.GREATER,
                Token.TokenType.GREATER_EQUAL,
                Token.TokenType.LESS,
                Token.TokenType.LESS_EQUAL)) {
            Token operator = tokenStream.previous();
            Expr right = addition();

            expression = new Expr.Binary(
                    expression,
                    operator,
                    right
            );
        }

        return expression;
    }

    /**
     * Addition -> Term (Addop Term)*
     *
     * @return the parsed additive expression
     */
    private Expr addition() {
        Expr expression = term();

        while (tokenStream.match(Token.TokenType.PLUS, Token.TokenType.MINUS)) {
            Token operator = tokenStream.previous();
            Expr right = term();
            expression = new Expr.Binary(
                    expression,
                    operator,
                    right
            );
        }

        return expression;
    }

    /**
     * Term -> Unary (Mulop Unary)*
     *
     * @return the parsed multiplicative expression
     */
    private Expr term() {
        Expr expression = unary();

        while (tokenStream.match(Token.TokenType.STAR, Token.TokenType.SLASH, Token.TokenType.PERCENT)) {
            Token operator = tokenStream.previous();
            Expr right = unary();
            expression = new Expr.Binary(
                    expression,
                    operator,
                    right
            );
        }

        return expression;
    }

    /**
     * Unary    -> Unaryop Unary | Factor
     * Unaryop  -> "-" | "*" | "&" | "++" | "--"
     *
     * @return the parsed unary expression or factor
     */
    private Expr unary() {
        Token Unaryop;

        if (tokenStream.match(Token.TokenType.MINUS, Token.TokenType.STAR, Token.TokenType.AMPERSAND,
                Token.TokenType.PLUS_PLUS, Token.TokenType.MINUS_MINUS)) {
            Unaryop = tokenStream.previous();
            return new Expr.Unary(
                    Unaryop, unary()
            );
        }

        return factor();
    }

    /**
     * Factor -> "(" Expression ")" | IntegerLiteral | FloatLiteral | Identifier
     *
     * @return the parsed grouping, literal, or variable
     */
    private Expr factor() {
        if (tokenStream.match(Token.TokenType.IDENTIFIER)) {
            return new Expr.Variable(tokenStream.previous());
        }

        if (tokenStream.match(Token.TokenType.INTEGER_LITERAL,
                Token.TokenType.FLOAT_LITERAL)) {
            return new Expr.Literal(tokenStream.previous().literal());
        }

        if (tokenStream.match(Token.TokenType.LEFT_PAREN)) {
            Expr expression = parseExpression();

            tokenStream.consume(
                    Token.TokenType.RIGHT_PAREN,
                    "Missing ')' after expression"
            );

            return new Expr.Grouping(expression);
        }

        Token unexpected = tokenStream.peek();
        String found = unexpected.type() == Token.TokenType.EOF
                ? "end of input"
                : "'" + unexpected.lexeme() + "'";

        throw new ParserException(
                "Expected an identifier, number, or '(' but found " + found,
                unexpected,
                unexpected.lexeme().length()
        );
    }
}
