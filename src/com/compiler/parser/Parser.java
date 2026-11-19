package com.compiler.parser;

// Scanner imports
import com.compiler.scanner.Token;
import com.compiler.scanner.SourceReader;

// Exception imports
import com.compiler.exceptions.ParserException;

// AST Definition imports
import com.compiler.ast.Stmt;

// Java lib imports
import java.util.ArrayList;
import java.util.List;

public class Parser
{
    private final TokenStream tokenStream;
    private final SourceReader sourceReader;
    private final StatementParser statementParser;

    public Parser(List<Token> tokenList, SourceReader sourceReader)
    {
        tokenStream = new TokenStream(new ArrayList<>(tokenList));
        this.sourceReader = sourceReader;

        // Our statement parser relies only on
        // the TokenStream, and an ExpressionParser
        statementParser = new StatementParser(
                tokenStream,
                new ExpressionParser(
                        tokenStream
                )
        );
    }

    /**
     * Parses all statements in the token stream.
     * @return the parsed statements, or {@code null} if the stream is empty
     */
    public List<Stmt> parse() {
        try {
            if (tokenStream.isAtEnd()) {
                return null;
            }

            List<Stmt> parsedStatements = new ArrayList<>();

            while (!tokenStream.isAtEnd())
            {
                parsedStatements.add(statementParser.parseStatement());
            }

            return parsedStatements;
        } catch (ParserException e) {
            System.err.println(e.getMessage());
            e.printDiagnostic(sourceReader);
            throw e;
        }
    }
}
