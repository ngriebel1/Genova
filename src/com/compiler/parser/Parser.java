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

    /**
     * Statement types used in the parser
     * Similar to using visitor style statement types
     * @author Nicholas J. Griebel
     */
    public sealed interface Stmt
            permits Stmt.Block, Stmt.AssignmentStatement, Stmt.IfStatement {

        record Block(List<Stmt> statements) implements Stmt { }
        record AssignmentStatement(Expr.Assign assignment) implements Stmt { }
        record IfStatement(Expr condition, Stmt ifBody, Stmt elseBody) implements Stmt { }
    }

    /**
     * Expression types used in the parser
     * Similar to using visitor style expression types
     * @author Nicholas J. Griebel
     */
    public sealed interface Expr permits
            Expr.Variable, Expr.Assign, Expr.Binary,
            Expr.Literal, Expr.Grouping {
        default String format() {
            return PrettyPrinter.format(this);
        }

        // Variable assignments
        record Variable(Token name) implements Expr { }

        // Standard math (Parses mathematical expressions and grouping of mathematical expressions).
        record Assign(Expr.Variable target, Token Assignop, Expr value) implements Expr { }
        record Binary(Expr left, Token operator, Expr right) implements Expr { }
        record Literal(Object value) implements Expr { }
        record Grouping(Expr expression) implements Expr { }
    }

    /**
     * Primary public parse interface -- Used to start the parsing process
     * @return {@code Stmt}
     * @author Nicholas J. Griebel
     */
    public Stmt parse() {
        try {
            if (tokenStream.isAtEnd()) {
                // Return a null expression to indicate end of tokenList
                return null;
            }

            // Or return a parsed statement tree
            return parseStatement();

        } catch (ParserException e) {
            // Display exception diagnostics
            System.err.println(e.getMessage());
            e.printDiagnostic(sourceReader);
            throw e;
        }
    }

    /**
     * Statement -> Block | AssignmentStatement | IfStatement | CallStatement | ...
     * @return {@code Stmt}
     * @author Nicholas J. Griebel
     */
    public Stmt parseStatement() {
        if (tokenStream.match(Token.TokenType.LEFT_BRACE)) {
            return parseBlock();
        }

        if (tokenStream.match(Token.TokenType.IF)) {
            return parseIf();
        }

        // Fallback on assignment statements
        return parseAssignmentStatement();
    }

    /**
     * IfStatement -> IF "(" Expression ")" Statement (ElseStatement)?
     * @return {@code Stmt.IfStatement}
     * @author Nicholas J. Griebel
     */
    private Stmt parseIf() {
        // Get the token representing the start of the if statement
        Token statementStart = tokenStream.previous();

        // Match and consume LEFT_PAREN or throw an exception
        if (!tokenStream.match(Token.TokenType.LEFT_PAREN))
            throw new ParserException(
                    "Expected '(', got '" + tokenStream.peek().lexeme() + "'",
                    statementStart,
                    tokenStream.peek()
            );

        // Get the if condition
        Expr condition = parseExpression();

        // match and consume RIGHT_PAREN or throw an exception
        if (!tokenStream.match(Token.TokenType.RIGHT_PAREN))
            throw new ParserException(
                    "Unmatched parenthesis: Expected ')', got '" + tokenStream.peek().lexeme() + "'",
                    statementStart,
                    tokenStream.peek()
            );

        // Get the if block (then)
        Stmt ifBlock = parseStatement();
        Stmt elseBlock = null;

        // Is there an else part?
        if (tokenStream.match(Token.TokenType.ELSE))
        {
            // If so, assign its block to elseBlock
            elseBlock = parseStatement();
        }

        // Put the if statement onto the tree
        return new Stmt.IfStatement(
            condition, ifBlock, elseBlock
        );
    }

    /**
     * Block -> "{" Statement* "}"
     *
     * @return A {@code List<Stmt>} of statements included in the block
     * @author Nicholas J. Griebel
     */
    private Stmt parseBlock() {
        // Get the opening brace token for exception reporting
        Token openingBrace = tokenStream.previous();
        List<Stmt> statements = new ArrayList<>();

        // Continue to collect and add statements to the statements list
        // until we reach our outer brace.
        while (!tokenStream.isAtEnd() && !tokenStream.check(Token.TokenType.RIGHT_BRACE)) {
            statements.add(parseStatement());
        }

        // If the while loop was broken by an EOF,
        if (tokenStream.isAtEnd())
            throw new ParserException(
                    "Unclosed block; Expected '}' before end of input.",
                    openingBrace,
                    openingBrace.lexeme().length()
            );

        // Consume the trailing right brace
        tokenStream.consume(Token.TokenType.RIGHT_BRACE, "Expected '}', got " + tokenStream.peek().lexeme());

        // Add the block statement to the tree
        return new Stmt.Block(
                statements
        );
    }

    /**
     * AssignmentStatement -> IDENTIFIER Assignop Assignment ";"
     *
     * @return Stmt.AssignmentStatement
     * @author Nicholas J. Griebel
     */
    private Stmt parseAssignmentStatement() {
        // Get our starting statement token for exception reporting
        Token statementStart = tokenStream.peek();
        // Retrieve our assignment expression
        Expr expression = assignment();
        // Get our ending expression token for exception reporting (range-based)
        Token statementEnd = tokenStream.previous();

        if (expression instanceof Expr.Assign assign) {
            if (!(tokenStream.match(Token.TokenType.SEMICOLON))) {
                String found = tokenStream.isAtEnd()
                        ? "end of input"
                        : "'" + tokenStream.peek().lexeme() + "'";
                Token.Location insertionPoint = new Token.Location(
                        statementEnd.at().line(),
                        statementEnd.at()._char() + statementEnd.lexeme().length()
                );
                throw new ParserException(
                        "Expected semicolon before " + found,
                        insertionPoint,
                        0
                );
            }

            return new Stmt.AssignmentStatement(
                    assign
            );
        }

        throw new ParserException(
                "Incorrect statement syntax at",
                statementStart,
                statementEnd
        );
    }

    /**
     * Expression -> Assignment
     * @return {@code Expr}
     * @author Nicholas J. Griebel
     */
    public Expr parseExpression() {
        return assignment();
    }

    /**
     * Assignment   -> IDENTIFIER Assignop Assignment | Addition
     * Assignop     -> "=" | "+=" | "-=" | "*=" | "/=" | "%="
     * @return  {@code Expr.Assign }
     * @author Nicholas J. Griebel
     */
    private Expr assignment() {
        Token expressionStart = tokenStream.peek();

        // Always start with higher precedence parsing
        Expr expression = equality();
        Token expressionEnd = tokenStream.previous();

        // Make sure the next token is an Assignop
        if (tokenStream.match(
                Token.TokenType.EQUAL, Token.TokenType.PLUS_EQUAL,
                Token.TokenType.MINUS_EQUAL, Token.TokenType.STAR_EQUAL,
                Token.TokenType.SLASH_EQUAL, Token.TokenType.PERCENT_EQUAL
            )
        ) {
            // Get the assignment operator (Check header comment to see all valid Assignops)
            Token assignop = tokenStream.previous();

            // Our LHS expression was not a variable. Throw an exception!
            if (!(expression instanceof Expr.Variable variable)) {
                throw new ParserException(
                        "Invalid assignment syntax at",
                        expressionStart,
                        expressionEnd);
            }

            // Return our valid assignment to the tree
            return new Expr.Assign(variable, assignop, assignment());
        }

        return expression;
    }

    /**
     * Equality     -> Relational (Equalityop Relational)*
     * Equalityop   -> "==" | "!="
     * @return {@code Expr.Binary }
     * @author Nicholas J. Griebel
     */
    private Expr equality() {
        Expr expression = relational();                 // Parse LHS operand

        while (tokenStream.match(Token.TokenType.EQUAL_EQUAL, Token.TokenType.NOT_EQUAL)) {
            Token operator = tokenStream.previous();    // Get our equality operator
            Expr right = relational();                  // Parse RHS operand

            // Create our new expression
            expression = new Expr.Binary(
                    expression,
                    operator,
                    right
            );
        }

        // And send 'er home
        return expression;
    }

    /**
     * Relational       -> Addition (Relationalop Addition)*
     * Relationalop     -> ">" | ">=" | "<" | "<="
     * @return {@code Expr.Binary }
     * @author Nicholas J. Griebel
     */
    private Expr relational() {
        Expr expression = addition();               // Parse LHS operand

        while (tokenStream.match(
                Token.TokenType.GREATER,
                Token.TokenType.GREATER_EQUAL,
                Token.TokenType.LESS,
                Token.TokenType.LESS_EQUAL)) {
            Token operator = tokenStream.previous(); // Get our relational operator
            Expr right = addition();                 // Parse our RHS operand

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
     * @return {@code Expr.Binary }
     * @author Nicholas J. Griebel
     */
    private Expr addition() {
        // Always start with higher precedence parsing
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

        // Return the node
        return expression;
    }

    /**
     * Term -> Factor (Mulop Factor)*
     * @return {@code Expr.Binary}
     * @author Nicholas J. Griebel
     */
    private Expr term() {
        Expr expression = factor();

        // Continue parsing all RHS expressions until we are no longer a MUL, DIV or MOD.
        while (tokenStream.match(Token.TokenType.STAR, Token.TokenType.SLASH, Token.TokenType.PERCENT)) {
            Token operator = tokenStream.previous();
            Expr right = factor();
            expression = new Expr.Binary(
                    expression,
                    operator,
                    right
            );
        }

        return expression;
    }

    /**
     * Factor -> "(" Expression ")" | Number | Identifier
     * @return {@code Expr.Grouping} | {@code Expr.Literal} | {@code Expr.Variable}
     * @author Nicholas J. Griebel
     */
    private Expr factor() {
        // Are we an identifier?
        if (tokenStream.match(Token.TokenType.IDENTIFIER)) {
            return new Expr.Variable(tokenStream.previous());
        }

        // Are we a literal?
        if (tokenStream.match(Token.TokenType.INTEGER_LITERAL,
                Token.TokenType.FLOAT_LITERAL)) {
            // If so, an Expr.Literal belongs here.
            return new Expr.Literal(tokenStream.previous().literal());
        }

        // Otherwise, are we a grouping?
        if (tokenStream.match(Token.TokenType.LEFT_PAREN)) {
            // Get our inner expression
            Expr expression = parseExpression();

            // Look for the closing parenthesis
            tokenStream.consume(
                    Token.TokenType.RIGHT_PAREN,
                    "Missing ')' after expression"
            );

            return new Expr.Grouping(expression);
        }

        // We didn't get the expected token --
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
