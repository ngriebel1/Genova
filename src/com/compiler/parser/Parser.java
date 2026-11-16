package com.compiler.parser;

import com.compiler.scanner.Scanner;
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
            permits Stmt.Block,         Stmt.AssignmentStatement,
                    Stmt.IfStatement,   Stmt.VariableDeclarationStatement,
                    Stmt.FunctionDeclarationStatement {

        record Block(List<Stmt> statements) implements Stmt {
            public Block {
                // Make an immutable copy of the statements list
                statements = List.copyOf(statements);
            }
        }
        record AssignmentStatement(Expr.Assign assignment) implements Stmt { }
        record IfStatement(Expr condition, Stmt ifBody, Stmt elseBody) implements Stmt { }
        record Parameter(Token type, int ptrDepth, Token name, Expr defaultValue) { }

        record VariableDeclarationStatement(
                List<Token> modifiers,
                Token type,
                int ptrDepth,
                Token name,
                Expr value
        ) implements Stmt {
            public VariableDeclarationStatement {
                // Make an immutable copy of the modifiers list
                modifiers = List.copyOf(modifiers);
            }
        }
        record FunctionDeclarationStatement(
                List<Token> modifiers,
                Token type,
                int ptrDepth,
                Token name,
                List<Parameter> parameters,
                FunctionBody body
        ) implements Stmt{
            public FunctionDeclarationStatement {
                // Make an immutable copy of the modifiers and parameters lists
                modifiers = List.copyOf(modifiers);
                parameters = List.copyOf(parameters);
            }
        }
    }

    public sealed interface FunctionBody
            permits FunctionBody.BlockBody, FunctionBody.ExpressionBody {
        record BlockBody(Stmt.Block block) implements FunctionBody { }
        record ExpressionBody(Expr expression) implements FunctionBody { }
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
    public List<Stmt> parse() {
        try {
            if (tokenStream.isAtEnd()) {
                // Return a null expression to indicate end of tokenList
                return null;
            }

            List<Stmt> parsedStatements = new ArrayList<Stmt>();

            while (!tokenStream.isAtEnd())
            {
                // Continue adding parsed statements to the
                // list until we reach the EOF token
                parsedStatements.add(parseStatement());
            }

            return parsedStatements;
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
        // Block
        if (tokenStream.match(Token.TokenType.LEFT_BRACE)) {
            return parseBlock();
        }

        // If
        if (tokenStream.match(Token.TokenType.IF)) {
            return parseIf();
        }

        // Declaration
        if (isType() != null)
        {
            return parseDeclaration();
        }

        // Assignment
        return parseAssignmentStatement();
    }

    /**
     * DeclarationStatement -> Modifier* Type IDENTIFIER ( FunctionTail | VariableTail )
     * Modifier             -> CONST | STATIC | DYNAMIC
     * FunctionTail         -> "(" Parameters? ")" ( Block | "-> Expression )
     * Parameters           -> Parameter ("," Parameter)*
     * Parameter            -> Type IDENTIFIER ("=" Expression)?
     * VariableTail         -> ("=" Expression)? ";"
     * Type                 -> BaseType Pointer*
     * BaseType             -> CHAR | INT | FLOAT | DOUBLE | LONG | STRING | VOID | BOOL
     * Pointer              -> "*"
     *
     * @return  {@code Stmt}
     * @author Nicholas J. Griebel
     */
    private Stmt parseDeclaration() {
        Token first = tokenStream.previous();
        List<Token> modifiers = new ArrayList<>();
        int ptrDepth = 0;
        Token type = null;
        Token name = null;

        if (isModifier(first.type())) {
            // Add the first modifier
            modifiers.add(first);

            // Then get *the rest*
            while (tokenStream.match(
                    Token.TokenType.CONST,
                    Token.TokenType.STATIC,
                    Token.TokenType.DYNAMIC
            )) {
                // We must be able to handle multiple modifiers
                // for modifier chains such as static const
                modifiers.add(tokenStream.previous());
            }
        }

        type = isModifier(first.type())
                ? isType()
                : first;

        // type is null if no valid type was found in isType()
        if (type == null) {
            throw new ParserException(
                    "Unknown type name at",
                    tokenStream.previous(),
                    tokenStream.previous().lexeme().length()
            );
        }

        // Get a pointer depth count (how many '*' are present), and get
        // the variable identifier
        if (tokenStream.match(Token.TokenType.STAR))
        {
            do {
                ++ptrDepth;
            } while (tokenStream.match(Token.TokenType.STAR));

            name = checkIdentifier();
        }
        else
        {
            if (tokenStream.match(Token.TokenType.IDENTIFIER))
            {
                // This is not a pointer, but we got a valid identifier
                name = tokenStream.previous();
            }
            else
            {
                // it wasn't a valid identifier
                throw new ParserException(
                        "Expected identifier, got '" + tokenStream.peek().lexeme() + "' at",
                        tokenStream.peek(),
                        tokenStream.peek().lexeme().length()
                );
            }
        }

        // Determine if we're a function declaration or a variable declaration
        if (tokenStream.match(Token.TokenType.LEFT_PAREN)) {
            return parseFunctionTail(modifiers, type, ptrDepth, name);
        }
        else if (tokenStream.match(Token.TokenType.EQUAL, Token.TokenType.SEMICOLON)) {
            return parseVariableTail(modifiers, type, ptrDepth, name);
        }

        // Neither path was acceptable - We've got a
        // malformed syntax on our hands.
        throw new ParserException(
                "Invalid declaration syntax. Expected '(', '=' or ';' - Got '" + tokenStream.peek().lexeme() + "'",
                first,
                tokenStream.peek()
        );
    }

    /**
     * FunctionTail -> "(" Parameters? ")" ( Block | ARROW_DASH Expression ";" )
     * @param modifiers A {@code List<Token>} of applicable variable/return type modifiers
     * @param type A {@code Token} containing the desired type for declaration
     * @param ptrDepth An {@code int} representing the depth of pointer declared (i.e int *x = 1, int **x = 2, etc.)
     * @param name The desired name of the variable to be declared. Type {@code Token}
     * @return A {@code Stmt} representing a functions body
     */
    private Stmt parseFunctionTail(List<Token> modifiers, Token type, int ptrDepth, Token name)
    {
        List<Stmt.Parameter> parameters = parseParameters();
        tokenStream.consume(Token.TokenType.RIGHT_PAREN, "Expected ')' after parameters");

        // Are we...
        //    1: A standard block body
        // or 2: An arrow shortcut?
        FunctionBody body;
        if (tokenStream.match(Token.TokenType.LEFT_BRACE))
        {
            body = new FunctionBody.BlockBody((Stmt.Block)parseBlock());
        }
        else if (tokenStream.match(Token.TokenType.ARROW_DASH))
        {
            Expr expression = parseExpression();
            tokenStream.consume(Token.TokenType.SEMICOLON, "Expected ';' after expression");
            body = new FunctionBody.ExpressionBody(expression);
        }
        else
        {
            Token unexpected = tokenStream.peek();

            String found = unexpected.type() == Token.TokenType.EOF
                    ? "end of input"
                    : "'" + unexpected.lexeme() + "'";

            throw new ParserException(
                    "Expected '{' or '->', got " + found,
                    unexpected,
                    unexpected.lexeme().length()
            );
        }

        return new Stmt.FunctionDeclarationStatement(
                modifiers, type, ptrDepth, name, parameters, body
        );
    }

    private List<Stmt.Parameter> parseParameters()
    {
        List<Stmt.Parameter> parameters = new ArrayList<>();

        if (tokenStream.check(Token.TokenType.RIGHT_PAREN))
        {
            return parameters; // Returning empty list on empty params
        }

        do
        {
            parameters.add(parseParameter());
        } while (tokenStream.match(Token.TokenType.COMMA));

        return parameters;
    }

    private Stmt.Parameter parseParameter()
    {
        Token type;
        int ptrDepth = 0;
        Token name;
        Expr defaultValue = null;

        // Get the type name, throw exception if it's null (bad syntax)
        if ((type = isType()) == null)
        {
            throw new ParserException(
                    "Missing type declaration at",
                    tokenStream.peek(),
                    tokenStream.peek().lexeme().length()
            );
        }

        // Get pointer declarations
        while (tokenStream.match(Token.TokenType.STAR))
        {
            ++ptrDepth;
        }

        if (!(tokenStream.match(Token.TokenType.IDENTIFIER)))
        {
            throw new ParserException(
                    "Invalid/Missing identifier at",
                    tokenStream.peek(),
                    tokenStream.peek().lexeme().length()
            );
        }

        // Get the identifier
        name = tokenStream.previous();

        // Get the optional default value
        if (tokenStream.match(Token.TokenType.EQUAL))
        {
            defaultValue = parseExpression();
        }

        return new Stmt.Parameter(
                type, ptrDepth, name, defaultValue
        );
    }

    /**
     *
     * @param modifiers A {@code List<Token>} of valid variable modifiers (E.G static const)
     * @param type A {code Token} containing the desired type of the variable to be declared
     * @param ptrDepth An {code int} representing the depth of pointer declared (e.g. int *x;(1), int **x;(2))
     * @param name The desired name of the variable to be declared. Type {@code Token}
     * @return A {@code Stmt} representing a variable declaration body (E.G int *x = 33; or int *y)
     */
    private Stmt parseVariableTail(List<Token> modifiers, Token type, int ptrDepth, Token name)
    {
        // Declaration with definition
        if (tokenStream.previous().type() == Token.TokenType.EQUAL)
        {
            // Get the definition expression
            Expr expression = parseExpression();

            tokenStream.consume(Token.TokenType.SEMICOLON, "Missing semicolon at");

            return new Stmt.VariableDeclarationStatement(
                    modifiers, type, ptrDepth, name, expression
            );
        }

        // Declaration without definition
        else if (tokenStream.previous().type() == Token.TokenType.SEMICOLON)
        {
            return new Stmt.VariableDeclarationStatement(
                    modifiers, type, ptrDepth, name, null
            );
        }

        Token unexpected = tokenStream.peek();

        String found = unexpected.type() == Token.TokenType.EOF
                ? "end of input"
                : "'" + unexpected.lexeme() + "'";

        throw new ParserException(
                "Expected '=' or ';', got " + found,
                unexpected,
                unexpected.lexeme().length()
        );
    }

    Token checkIdentifier()
    {
        if (!(tokenStream.match(Token.TokenType.IDENTIFIER))) {
            // No identifier found -- Is it a reserved word?
            if (Scanner.KEYWORDS.containsValue(tokenStream.peek().type())) {
                throw new ParserException(
                        "Invalid identifier (Reserved word) at",
                        tokenStream.peek(),
                        tokenStream.peek().lexeme().length()
                );
            }

            throw new ParserException(
                    "Expected identifier, got '" + tokenStream.peek().lexeme() + "' at",
                    tokenStream.peek(),
                    tokenStream.peek().lexeme().length()
            );
        }

        return tokenStream.previous();
    }

    private Token isType()
    {
        // Are we a valid type?
        if (tokenStream.match(
                Token.TokenType.CHAR,
                Token.TokenType.INT,
                Token.TokenType.DOUBLE,
                Token.TokenType.FLOAT,
                Token.TokenType.BOOL,
                Token.TokenType.STRING,
                Token.TokenType.AUTO,
                Token.TokenType.LONG,
                Token.TokenType.VOID,
                Token.TokenType.CONST,
                Token.TokenType.STATIC,
                Token.TokenType.DYNAMIC
        ))
        {
            // Return it if it's a valid type
            return tokenStream.previous();
        }

        return null;
    }

    /**
     * Determine whether a given TokenType is a variable type modifier
     * @return A null token if not modifier, otherwise returns the matched token
     * @author Nicholas J. Griebel
     */
    private boolean isModifier(Token.TokenType t)
    {
        return t == Token.TokenType.CONST
                || t == Token.TokenType.DYNAMIC
                || t == Token.TokenType.STATIC;
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
    private Stmt.Block parseBlock() {
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
