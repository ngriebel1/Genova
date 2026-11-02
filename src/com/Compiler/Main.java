package com.compiler;

import java.util.List;
import java.util.ArrayList;

import com.compiler.exceptions.ScannerException;
import com.compiler.scanner.Scanner;
import com.compiler.scanner.Token;


public class Main
{
    /**
     * @author Nicholas Griebel
     * Main method used for debugging
     */
    public static void main(String[] args)
    {
        Scanner newScanner = new Scanner(
                // PLUS MINUS STAR SLASH MINUS_EQUALS STAR_EQUALS SLASH_EQUALS STAR STAR STAR PLUS_EQUALS STAR_EQUALS SLASH_EQUALS
                "+ - * / += \n-= *= /= /* This is a block comment.// Single line inside multi line test \nIt \nspans \nmultiple \nlines. */ * * * += *= /= "
        );

        final List<Token> newList;
        try
        {
            newList = newScanner.scanLine();
            for (var token : newList)
            {
                System.out.println(token.toString());
            }

            System.out.println("newList size = " + newList.size());
        }
        catch(ScannerException e)
        {
            System.err.println("Caught a ScannerException! Message below: ");
            System.err.println(e.getMessage());
        }
    }

}
