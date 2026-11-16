package com.compiler;

import com.compiler.exceptions.ParserException;
import com.compiler.parser.Parser;
import com.compiler.parser.PrettyPrinter;
import com.compiler.scanner.Scanner;
import java.util.List;
import java.util.ArrayList;


/**
 * Currently, the {@code Main} class is a simple debug driver for the
 * {@link Scanner} class. It defines a test string, instantiates a {@link Scanner}
 * object, and initiates a scan of the string. Tokens that are retrieved from the
 * source file are printed to standard output via System.out.println().
 */
public class Main {
    /**
     * @author Nicholas Griebel
     * Main method used for debugging
     */
    static void main() {
        String testString = (
                """
                static const int func() {
                    static int x = 33;
                
                    if (x > 10) {
                        if (x == 33)
                        {
                            // How'd X get larger than 10? get back here...
                            x = 34;
                        }
                    }
                    else {
                        x = 10;
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
            List<Parser.Stmt> parsedStatements = new ArrayList<Parser.Stmt>();
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
