package com.compiler.ast;

import com.compiler.parser.PrettyPrinter;
import com.compiler.scanner.Token;

/**
 * Expression node types produced by the parser.
 * @author Nicholas J. Griebel
 */
public sealed interface Expr permits
        Expr.Variable, Expr.Assign, Expr.Binary,
        Expr.Literal, Expr.Grouping, Expr.Unary {
    default String format() {
        return PrettyPrinter.format(this);
    }

    record Variable(Token name) implements Expr { }

    record Unary(Token Unaryop, Expr value) implements Expr { }
    record Assign(Expr target, Token Assignop, Expr value) implements Expr { }
    record Binary(Expr left, Token operator, Expr right) implements Expr { }
    record Literal(Object value) implements Expr { }
    record Grouping(Expr expression) implements Expr { }
}
