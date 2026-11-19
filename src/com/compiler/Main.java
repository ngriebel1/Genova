package com.compiler;

import com.compiler.exceptions.ParserException;
import com.compiler.parser.Parser;
import com.compiler.parser.PrettyPrinter;
import com.compiler.scanner.Scanner;
import com.compiler.ast.Stmt;

import java.util.List;
import java.util.ArrayList;


/**
 * Debug driver that scans and parses a sample program, then prints its syntax tree.
 */
public class Main {
    /**
     * Runs the sample scanner/parser demonstration.
     */
    static void main() {
        String testString = (
                """
                static const int *func() {
                    int *x = 33;
                    int y = 97;
                
                    *x = 3;
                1
                    if (x > 10) {
                        if (x == 33)
                        {
                            x = 34;
                        }
                    }
                    else {
                    // TODO: Assignment Statements are rejecting UNARY expressions
                    //      due to the assignment statement explicitly relying on Expr.Variable
                    //      instead of allowing Expr (general). Change this to allow unary 
                    //      expressions to act as proper LHS values in an assignment statement.
                        x = y;
                    }
                }
                """
        );

        Scanner scanner = new Scanner(testString);
        Parser parser = new Parser(scanner.scan(), scanner.getReader());

        if (scanner.hasErrors())
        {
            return;
        }

        try
        {
            List<Stmt> parsedStatements = new ArrayList<Stmt>();
            parsedStatements = parser.parse();

            for (var statement : parsedStatements)
            {
                System.out.println(
                        PrettyPrinter.format(statement)
                );
            }
        }
        catch (ParserException ignored)
        {
            // Parser.parse() has already printed the source diagnostic.
            // We don't need to do anything with this exception yet
        }
        finally
        {
            System.out.println();
            System.out.println("The Genova Programming Language");
            System.out.println("Written by Nicholas J. Griebel, (C)2026");
        }
    }
}
