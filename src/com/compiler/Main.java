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
                xCol * yCol % (xCol * yCol)
                """
        );

        Scanner scanner = new Scanner(testString);
        Parser parser = new Parser(scanner.scan(), scanner.getReader());

        if (scanner.hasErrors())
        {
            return;
        }

        try {
            System.out.println(PrettyPrinter.format(parser.parse()));
        } catch (ParserException ignored) {
            // Parser.parse() has already printed the source diagnostic.
        }
    }
}
