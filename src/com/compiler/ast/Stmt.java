package com.compiler.ast;

import com.compiler.scanner.Token;

import java.util.List;

/**
 * Statement node types produced by the parser.
 * @author Nicholas J. Griebel
 */
public sealed interface Stmt
        permits Stmt.Block,         Stmt.AssignmentStatement,
        Stmt.IfStatement,   Stmt.VariableDeclarationStatement,
        Stmt.FunctionDeclarationStatement {

    record Block(List<Stmt> statements) implements Stmt {
        public Block {
            // Make an immutable copy of the statements list
            statements = List.copyOf(statements);
        }
    }
    record AssignmentStatement(Expr assignment) implements Stmt { }
    record IfStatement(Expr condition, Stmt ifBody, Stmt elseBody) implements Stmt { }
    record Parameter(Token type, int ptrDepth, Token name, Expr defaultValue) { }

    record VariableDeclarationStatement(
            List<Token> modifiers,
            Token type,
            int ptrDepth,
            Token name,
            Expr value
    ) implements Stmt {
        public VariableDeclarationStatement {
            // Make an immutable copy of the modifiers list
            modifiers = List.copyOf(modifiers);
        }
    }
    record FunctionDeclarationStatement(
            List<Token> modifiers,
            Token type,
            int ptrDepth,
            Token name,
            List<Parameter> parameters,
            FunctionBody body
    ) implements Stmt{
        public FunctionDeclarationStatement {
            // Make an immutable copy of the modifiers and parameters lists
            modifiers = List.copyOf(modifiers);
            parameters = List.copyOf(parameters);
        }
    }
}