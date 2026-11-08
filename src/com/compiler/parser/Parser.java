package com.compiler.parser;

import com.compiler.scanner.Token;
import com.compiler.exceptions.ParserException;
import com.compiler.scanner.SourceReader;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final TokenStream tokenStream;
    private final SourceReader sourceReader;

    public Parser(List<Token> tokenList, SourceReader sourceReader) {
        tokenStream = new TokenStream(new ArrayList<>(tokenList));
        this.sourceReader = sourceReader;
    }

    public sealed interface Expr permits Expr.Variable, Expr.Assign, Expr.Binary, Expr.Literal, Expr.Grouping {
        default String format() {
            return PrettyPrinter.format(this);
        }

        // Variable assignments
        record Variable(Token name) implements Expr { }

        // Standard math (Parses mathematical expressions and grouping of mathematical expressions).
        record Assign(Expr.Variable target, Expr value) implements Expr { }
        record Binary(Expr left, Token operator, Expr right) implements Expr { }
        record Literal(Object value) implements Expr { }
        record Grouping(Expr expression) implements Expr { }
    }


    public Expr parseExpression() {
        return assignment();
    }

    public Expr parse() {
        try
        {
            return parseExpression();
        }
        catch (ParserException e)
        {
            System.err.println(e.getMessage());
            e.printDiagnostic(sourceReader);
            throw e;
        }
    }

    private Expr assignment() {
        // Always start with higher precedence parsing
        Expr expression = addition();

        // Make sure the next token is an EQUAL
        if (tokenStream.match(Token.TokenType.EQUAL))
        {
            Token equals = tokenStream.previous();

            // Our LHS expression was not a variable. Throw an exception!
            if (!(expression instanceof Expr.Variable))
            {
                throw new ParserException("Invalid assignment syntax", equals, equals.lexeme().length());
            }

            // Cast our expression to the correct Expr.Variable record, and
            // get our RHS value (which could be another assignment, as in
            // a = b = c ). Return the resulting Assign Expression.
            return new Expr.Assign((Expr.Variable)expression, assignment());
        }

        return expression;
    }

    private Expr addition() {
        // Always start with higher precedence parsing
        Expr expression = term();

        // Continue parsing additions as long as we keep encountering TokenType.PLUS
        // or TokenType.MINUS. Inside the loop, we are parsing all available RHS
        // expressions: For example 3 + 4 + 5 - 6;
        // 3 is our LHS value which was parsed using our term() call, and + 4,
        // + 5, and - 6 are parsed in this loop
        while (tokenStream.match(Token.TokenType.PLUS, Token.TokenType.MINUS)) {
            // Get the math operator from the token stream
            Token operator = tokenStream.previous();
            // Parse the term on the right side of the operator
            Expr right = term();

            // Note that in this assignment, instead of visualizing a reassignment,
            // visualizing an addition to the tree is more valid. We are adding what
            // we just parsed to the existing AST tree.
            expression = new Expr.Binary(expression, operator, right);
        }

        // Return the node
        return expression;
    }

    private Expr term() {
        Expr expression = factor();

        // Continue parsing all RHS expressions until we are no longer a MUL, DIV or MOD.
        while (tokenStream.match(Token.TokenType.STAR, Token.TokenType.SLASH, Token.TokenType.PERCENT)) {
            Token operator = tokenStream.previous();
            Expr right = factor();
            expression = new Expr.Binary(expression, operator, right);
        }

        // Return the expression we've created. Note: There are two potential
        // paths here.
        //
        // 1: Our first path is one where there was no available
        //      MUL, DIV or MOD expressions to parse. In this case,
        //      we simply pass the expression that we got from factor()
        //      back down to addition(), which itself is still working through
        //      all possible RHS expressions. In this case, we are probably
        //      an addition or subtraction where we are simply returning
        //      a literal here.
        // 2: Our second path is one where a MUL, DIV, or MOD (or multiple)
        //      was/were available. In this case, the previous while statement
        //      handles all possible RHS expressions at this precedence level, adds
        //      the expressions to the tree, and then passes progress so far back down
        //      to addition(). It will loop again if we encounter an add/sub after this,
        //      followed by another mul/div/mod
        //
        // NOTE: The above thoughts are mine, but do not exactly represent what is happening.
        //      A better way to understand it would be:
        //          `Return the factor or multiplication/division/modulo expression built here.
        //          addition() uses this as a term, either as its initial expression or as
        //          the right-hand side of a + or -. `
        return expression;
    }

    private Expr factor() {
        // Are we an identifier?
        if (tokenStream.match(Token.TokenType.IDENTIFIER))
        {
            return new Expr.Variable(tokenStream.previous());
        }

        // Are we a leaf? Either float or integer?
        if (tokenStream.match(Token.TokenType.INTEGER_LITERAL,
                Token.TokenType.FLOAT_LITERAL)) {
            // If so, an Expr.Literal belongs here.
            return new Expr.Literal(tokenStream.previous().literal());
        }

        // Otherwise, are we a grouping?
        if (tokenStream.match(Token.TokenType.LEFT_PAREN)) {
            // We need a new branch -- grow it using parseExpression which
            // starts a new Expression branch. Then, the only thing we need
            // to be worried about is the closing parenthesis and returning
            // the expression to the caller
            Expr expression = parseExpression();

            // Look for the closing parenthesis
            tokenStream.consume(
                    Token.TokenType.RIGHT_PAREN,
                    "Missing ')' after expression"
            );

            return new Expr.Grouping(expression);
        }

        // If we fell through it means we didn't find the right token.
        // There must be a syntactical mistake here. Throw an exception
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
