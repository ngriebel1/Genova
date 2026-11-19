package com.compiler.parser;

// Scanner imports
import com.compiler.scanner.Scanner;
import com.compiler.scanner.Token;

// Exception imports
import com.compiler.exceptions.ParserException;

// AST Structure imports
import com.compiler.ast.Expr;
import com.compiler.ast.FunctionBody;
import com.compiler.ast.Stmt;

// Java lib imports
import java.util.ArrayList;
import java.util.List;

public class StatementParser {
    private final TokenStream tokenStream;
    private final ExpressionParser expressionParser;

    public StatementParser(TokenStream tokenStream, ExpressionParser expressionParser)
    {
        this.tokenStream = tokenStream;
        this.expressionParser = expressionParser;
    }

    /**
     * Parses a block, conditional, declaration, or assignment statement.
     *
     * @return the parsed statement
     */
    public Stmt parseStatement() {
        if (tokenStream.match(Token.TokenType.LEFT_BRACE)) {
            return parseBlock();
        }

        if (tokenStream.match(Token.TokenType.IF)) {
            return parseIf();
        }

        if (isType() != null) {
            return parseDeclaration();
        }

        return parseAssignmentStatement();
    }

    /**
     * Declaration -> Modifier* Type IDENTIFIER ( FunctionTail | VariableTail )
     * Modifier             -> CONST | STATIC | DYNAMIC
     * FunctionTail         -> "(" Parameters? ")" ( Block | "-> Expression )
     * Parameters           -> Parameter ("," Parameter)*
     * Parameter            -> Type IDENTIFIER ("=" Expression)?
     * VariableTail         -> ("=" Expression)? ";"
     * Type                 -> BaseType Pointer*
     * BaseType             -> CHAR | INT | FLOAT | DOUBLE | LONG | STRING | VOID | BOOL | AUTO
     * Pointer              -> "*"
     *
     * @return {@code Stmt}
     * @author Nicholas J. Griebel
     */
    private Stmt parseDeclaration() {
        Token first = tokenStream.previous();
        List<Token> modifiers = new ArrayList<>();
        int ptrDepth = 0;
        Token type;
        Token name;

        if (isModifier(first.type())) {
            modifiers.add(first);

            while (tokenStream.match(
                    Token.TokenType.CONST,
                    Token.TokenType.STATIC,
                    Token.TokenType.DYNAMIC
            )) {
                modifiers.add(tokenStream.previous());
            }
        }

        type = isModifier(first.type())
                ? isType()
                : first;

        if (type == null) {
            throw new ParserException(
                    "Unknown type name at",
                    tokenStream.previous(),
                    tokenStream.previous().lexeme().length()
            );
        }

        if (tokenStream.match(Token.TokenType.STAR)) {
            do {
                ++ptrDepth;
            } while (tokenStream.match(Token.TokenType.STAR));

            name = checkIdentifier();
        } else {
            if (tokenStream.match(Token.TokenType.IDENTIFIER)) {
                name = tokenStream.previous();
            } else {
                throw new ParserException(
                        "Expected identifier, got '" + tokenStream.peek().lexeme() + "' at",
                        tokenStream.peek(),
                        tokenStream.peek().lexeme().length()
                );
            }
        }

        if (tokenStream.match(Token.TokenType.LEFT_PAREN)) {
            return parseFunctionTail(modifiers, type, ptrDepth, name);
        } else if (tokenStream.match(Token.TokenType.EQUAL, Token.TokenType.SEMICOLON)) {
            return parseVariableTail(modifiers, type, ptrDepth, name);
        }

        throw new ParserException(
                "Invalid declaration syntax. Expected '(', '=' or ';' - Got '" + tokenStream.peek().lexeme() + "'",
                first,
                tokenStream.peek()
        );
    }

    /**
     * FunctionTail -> "(" Parameters? ")" ( Block | ARROW_DASH Expression ";" )
     *
     * @param modifiers declaration modifiers
     * @param type      declared return type
     * @param ptrDepth  pointer depth of the return type
     * @param name      function name
     * @return the function declaration
     */
    private Stmt parseFunctionTail(List<Token> modifiers, Token type, int ptrDepth, Token name) {
        List<Stmt.Parameter> parameters = parseParameters();
        tokenStream.consume(Token.TokenType.RIGHT_PAREN, "Expected ')' after parameters");

        FunctionBody body;
        if (tokenStream.match(Token.TokenType.LEFT_BRACE)) {
            body = new FunctionBody.BlockBody(parseBlock());
        } else if (tokenStream.match(Token.TokenType.ARROW_DASH)) {
            Expr expression = expressionParser.parseExpression();
            tokenStream.consume(Token.TokenType.SEMICOLON, "Expected ';' after expression");
            body = new FunctionBody.ExpressionBody(expression);
        } else {
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

    private List<Stmt.Parameter> parseParameters() {
        List<Stmt.Parameter> parameters = new ArrayList<>();

        if (tokenStream.check(Token.TokenType.RIGHT_PAREN)) {
            return parameters; // Returning empty list on empty params
        }

        do {
            parameters.add(parseParameter());
        } while (tokenStream.match(Token.TokenType.COMMA));

        return parameters;
    }

    private Stmt.Parameter parseParameter() {
        Token type;
        int ptrDepth = 0;
        Token name;
        Expr defaultValue = null;

        // Get the type name, throw exception if it's null (bad syntax)
        if ((type = isType()) == null) {
            throw new ParserException(
                    "Missing type declaration at",
                    tokenStream.peek(),
                    tokenStream.peek().lexeme().length()
            );
        }

        // Get pointer declarations
        while (tokenStream.match(Token.TokenType.STAR)) {
            ++ptrDepth;
        }

        if (!(tokenStream.match(Token.TokenType.IDENTIFIER))) {
            throw new ParserException(
                    "Invalid/Missing identifier at",
                    tokenStream.peek(),
                    tokenStream.peek().lexeme().length()
            );
        }

        // Get the identifier
        name = tokenStream.previous();

        // Get the optional default value
        if (tokenStream.match(Token.TokenType.EQUAL)) {
            defaultValue = expressionParser.parseExpression();
        }

        return new Stmt.Parameter(
                type, ptrDepth, name, defaultValue
        );
    }

    /**
     *
     * @param modifiers declaration modifiers
     * @param type      declared variable type
     * @param ptrDepth  pointer depth of the declared type
     * @param name      variable name
     * @return the variable declaration
     */
    private Stmt parseVariableTail(List<Token> modifiers, Token type, int ptrDepth, Token name) {
        // Declaration with definition
        if (tokenStream.previous().type() == Token.TokenType.EQUAL) {
            Expr expression = expressionParser.parseExpression();

            tokenStream.consume(Token.TokenType.SEMICOLON, "Missing semicolon at");

            return new Stmt.VariableDeclarationStatement(
                    modifiers, type, ptrDepth, name, expression
            );
        }

        // Declaration without definition
        else if (tokenStream.previous().type() == Token.TokenType.SEMICOLON) {
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

    Token checkIdentifier() {
        if (!(tokenStream.match(Token.TokenType.IDENTIFIER))) {
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

    private Token isType() {
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
        )) {
            return tokenStream.previous();
        }

        return null;
    }

    /**
     * Determines whether a token type is a declaration modifier.
     */
    private boolean isModifier(Token.TokenType t) {
        return t == Token.TokenType.CONST
                || t == Token.TokenType.DYNAMIC
                || t == Token.TokenType.STATIC;
    }

    /**
     * Parses an {@code if} statement and its optional {@code else} branch.
     */
    private Stmt parseIf() {
        Token statementStart = tokenStream.previous();

        if (!tokenStream.match(Token.TokenType.LEFT_PAREN))
            throw new ParserException(
                    "Expected '(', got '" + tokenStream.peek().lexeme() + "'",
                    statementStart,
                    tokenStream.peek()
            );

        Expr condition = expressionParser.parseExpression();

        if (!tokenStream.match(Token.TokenType.RIGHT_PAREN))
            throw new ParserException(
                    "Unmatched parenthesis: Expected ')', got '" + tokenStream.peek().lexeme() + "'",
                    statementStart,
                    tokenStream.peek()
            );

        Stmt ifBlock = parseStatement();
        Stmt elseBlock = null;

        if (tokenStream.match(Token.TokenType.ELSE)) {
            elseBlock = parseStatement();
        }

        return new Stmt.IfStatement(
                condition, ifBlock, elseBlock
        );
    }

    /**
     * Block -> "{" Statement* "}"
     *
     * @return the parsed block
     */
    private Stmt.Block parseBlock() {
        Token openingBrace = tokenStream.previous();
        List<Stmt> statements = new ArrayList<>();

        while (!tokenStream.isAtEnd() && !tokenStream.check(Token.TokenType.RIGHT_BRACE)) {
            statements.add(parseStatement());
        }

        if (tokenStream.isAtEnd())
            throw new ParserException(
                    "Unclosed block; Expected '}' before end of input.",
                    openingBrace,
                    openingBrace.lexeme().length()
            );

        tokenStream.consume(Token.TokenType.RIGHT_BRACE, "Expected '}', got " + tokenStream.peek().lexeme());

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
        Expr expression = expressionParser.parseExpression();
        // Get our ending expression token for exception reporting (range-based)
        Token statementEnd = tokenStream.previous();

        if (expression instanceof Expr.Assign || expression instanceof Expr.Unary) {
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
                    expression
            );
        }

        throw new ParserException(
                "Incorrect statement syntax at",
                statementStart,
                statementEnd
        );
    }
}

