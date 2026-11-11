package com.compiler;

import com.compiler.exceptions.ParserException;
import com.compiler.parser.Parser;
import com.compiler.parser.PrettyPrinter;
import com.compiler.scanner.Scanner;


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
                if (x == 33)
                {
                    x = 10;
                }
                else
                {
                    x = 20;
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
            com.compiler.parser.Parser.Stmt parsedStatement;
            while ((parsedStatement = parser.parse()) != null)
            {
                System.out.println("Parsing next statement...");
                System.out.println(PrettyPrinter.format(parsedStatement));
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
