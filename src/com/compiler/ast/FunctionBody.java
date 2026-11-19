package com.compiler.ast;

public sealed interface FunctionBody
        permits FunctionBody.BlockBody, FunctionBody.ExpressionBody {

    record BlockBody(Stmt.Block block) implements FunctionBody { }
    record ExpressionBody(Expr expression) implements FunctionBody { }
}
