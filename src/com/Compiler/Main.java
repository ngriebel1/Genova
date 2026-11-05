package com.compiler;

import java.util.List;
import java.util.ArrayList;

import com.compiler.exceptions.ScannerException;
import com.compiler.scanner.Scanner;
import com.compiler.scanner.Token;
import com.compiler.scanner.SourceReader;


public class Main {
    /**
     * @author Nicholas Griebel
     * Main method used for debugging
     */
    public static void main(String[] args) {
        String testString = "int x = 42; double pi = 3.14;";

        Scanner scanner = new Scanner(testString);
        List<Token> tokenList = new ArrayList<>();

        while (!scanner.isAtEnd()) {
            try {
                System.out.println(" === Scanning New Line ===");
                tokenList = scanner.scanLine();
                for (var token : tokenList) {
                    System.out.println(token.toString());
                }
            } catch (ScannerException e) {
                System.err.println(e.getMessage());
                e.printDiagnostic(scanner.getReader());
            }
        }
    }
}
