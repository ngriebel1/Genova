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
        String testString =
                """
                string intro = "Hello, World! This is a string. ";
                double x = 3.14;
                double y = 9.96;
                
                double z = x * y;
                
                printf(intro);
                printf("x * y = " + z);
                
                if (z == 20.00)
                {
                    if (y == 10.0 && x == 2.0)
                    {
                        printf("Oh, I see. Y was 10 and x was 2. Not weird!\\n");
                        return 0;
                    }
                    else if (x == 10.0 && y == 2.0)
                    {
                        printf("Oh, I see. X was 10 and Y was 2. Not weird!\\n");
                        return 0;
                    }
                    else
                    {
                        printf("Black magic!\\n");
                        return -1;
                    }
                }
                """;

        Scanner scanner = new Scanner(testString);
        List<Token> tokenList = new ArrayList<>();

        tokenList = scanner.scan();

        for (var token : tokenList)
        {
            System.out.println(token.toString());
        }
    }
}
