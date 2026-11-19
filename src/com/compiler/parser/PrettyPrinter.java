package com.compiler.parser;

import com.compiler.scanner.Token;
import com.compiler.ast.Stmt;
import com.compiler.ast.Expr;
import com.compiler.ast.FunctionBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Renders expression and statement trees as top-down, textbook-style trees.
 *
 * @author Github Copilot
 */
public final class PrettyPrinter {
    private PrettyPrinter() {
    }

    private record TreeLayout(List<String> lines, int root, int width) {
    }

    public static String format(Expr expression) {
        return String.join(System.lineSeparator(), layout(expression).lines());
    }

    public static String format(Stmt statement) {
        return String.join(System.lineSeparator(), layout(statement).lines());
    }

    private static TreeLayout layout(Stmt statement) {
        if (statement instanceof Stmt.Block(List<Stmt> statements)) {
            List<TreeLayout> children = statements.stream()
                    .map(PrettyPrinter::layout)
                    .toList();
            return layoutNode("Block", children);
        }

        if (statement instanceof Stmt.AssignmentStatement(Expr expression)) {
            return layoutNode(
                    "AssignmentStatement",
                    List.of(layout(expression))
            );
        }

        if (statement instanceof Stmt.VariableDeclarationStatement(
                List<Token> modifiers, Token type, int ptrDepth, Token name, Expr value
        )) {
            List<TreeLayout> children = layoutDeclarationHeader(
                    modifiers,
                    type,
                    ptrDepth,
                    name
            );
            children.add(value == null
                    ? layoutNode("Initializer: none", List.of())
                    : layoutNode("Initializer", List.of(layout(value))));
            return layoutNode("VariableDeclarationStatement", children);
        }

        if (statement instanceof Stmt.FunctionDeclarationStatement(
                List<Token> modifiers, Token type, int ptrDepth, Token name, List<Stmt.Parameter> parameters,
                FunctionBody body
        )) {
            List<TreeLayout> children = layoutDeclarationHeader(
                    modifiers,
                    type,
                    ptrDepth,
                    name
            );
            children.add(layout(body));
            return layoutNode("FunctionDeclarationStatement", children);
        }

        if (statement instanceof Stmt.IfStatement(
                Expr condition, Stmt ifBody, Stmt elseBody
        )) {
            List<TreeLayout> children = new ArrayList<>();
            children.add(layoutNode("Condition", List.of(layout(condition))));
            children.add(layoutNode("Then", List.of(layout(ifBody))));
            if (elseBody != null) {
                children.add(layoutNode("Else", List.of(layout(elseBody))));
            }
            return layoutNode("IfStatement", children);
        }

        throw new IllegalStateException("Unknown statement type: " + statement.getClass());
    }

    private static List<TreeLayout> layoutDeclarationHeader(
            List<Token> modifiers,
            Token type,
            int pointerDepth,
            Token name
    ) {
        List<TreeLayout> children = new ArrayList<>();
        String modifierText = modifiers.isEmpty()
                ? "Modifiers: none"
                : "Modifiers: " + String.join(
                " ",
                modifiers.stream().map(Token::lexeme).toList()
        );
        children.add(layoutNode(modifierText, List.of()));
        children.add(layoutNode(
                "Type: " + type.lexeme() + "*".repeat(pointerDepth),
                List.of()
        ));
        children.add(layoutNode("Name: " + name.lexeme(), List.of()));
        return children;
    }

    private static TreeLayout layout(FunctionBody body) {
        if (body instanceof FunctionBody.BlockBody(Stmt.Block block)) {
            return layoutNode("BlockBody", List.of(layout(block)));
        }

        if (body instanceof FunctionBody.ExpressionBody(Expr expression)) {
            return layoutNode("ExpressionBody", List.of(layout(expression)));
        }

        throw new IllegalStateException("Unknown function body type: " + body.getClass());
    }

    private static TreeLayout layout(Expr expression) {
        if (expression instanceof Expr.Literal(Object value)) {
            String label = String.valueOf(value);
            return new TreeLayout(List.of(label), label.length() / 2, label.length());
        }

        if (expression instanceof Expr.Variable(Token name)) {
            String label = name.lexeme();
            return new TreeLayout(List.of(label), label.length() / 2, label.length());
        }

        if (expression instanceof Expr.Grouping(Expr expression1)) {
            TreeLayout child = layout(expression1);
            int childOffset = Math.max(0, "Grouping".length() / 2 - child.root());
            int root = childOffset + child.root();
            int width = Math.max("Grouping".length(), childOffset + child.width());
            List<String> lines = new ArrayList<>();
            lines.add(place("Grouping", width, root - "Grouping".length() / 2));
            lines.add(place("|", width, root));
            appendPadded(lines, child.lines(), childOffset, width);
            return new TreeLayout(lines, root, width);
        }

        if (expression instanceof Expr.Unary(Token unaryOperator, Expr value)) {
            return layoutNode("Unary: " + unaryOperator.lexeme(), List.of(layout(value)));
        }

        if (expression instanceof Expr.Assign(Expr target, Token assignop, Expr value)) {
            return layoutBinary(assignop.lexeme(), target, value);
        }

        if (expression instanceof Expr.Binary(Expr left, Token operator, Expr right)) {
            return layoutBinary(operator.lexeme(), left, right);
        }

        throw new IllegalStateException("Unknown expression type: " + expression.getClass());
    }

    private static TreeLayout layoutNode(String label, List<TreeLayout> children) {
        if (children.isEmpty()) {
            return new TreeLayout(List.of(label), label.length() / 2, label.length());
        }

        int[] offsets = new int[children.size()];
        int nextOffset = 0;
        for (int i = 0; i < children.size(); i++) {
            offsets[i] = nextOffset;
            nextOffset += children.get(i).width() + 4;
        }

        int firstRoot = offsets[0] + children.getFirst().root();
        int lastIndex = children.size() - 1;
        int lastRoot = offsets[lastIndex] + children.getLast().root();
        int root = (firstRoot + lastRoot) / 2;
        int leftPadding = Math.max(0, label.length() / 2 - root);
        int width = nextOffset - 4 + leftPadding;
        root += leftPadding;
        width = Math.max(width, root - label.length() / 2 + label.length());

        List<String> lines = new ArrayList<>();
        lines.add(place(label, width, root - label.length() / 2));
        lines.add(place("|", width, root));

        if (children.size() == 1) {
            appendPadded(lines, children.getFirst().lines(), leftPadding, width);
            return new TreeLayout(lines, root, width);
        }

        char[] branches = blankLine(width);
        int leftmostRoot = firstRoot + leftPadding;
        int rightmostRoot = lastRoot + leftPadding;
        Arrays.fill(branches, Math.min(root, leftmostRoot), Math.max(root, rightmostRoot) + 1, '-');
        lines.add(new String(branches));

        char[] childBranches = blankLine(width);
        for (int i = 0; i < children.size(); i++) {
            childBranches[offsets[i] + children.get(i).root() + leftPadding] = '|';
        }
        lines.add(new String(childBranches));

        int childStartRow = lines.size();
        for (int i = 0; i < children.size(); i++) {
            appendPaddedAt(
                    lines,
                    children.get(i).lines(),
                    offsets[i] + leftPadding,
                    width,
                    childStartRow
            );
        }

        return new TreeLayout(lines, root, width);
    }

    private static TreeLayout layoutBinary(String label, Expr leftExpression, Expr rightExpression) {
        TreeLayout left = layout(leftExpression);
        TreeLayout right = layout(rightExpression);
        int rightOffset = left.width() + 4;
        int leftRoot = left.root();
        int rightRoot = rightOffset + right.root();
        int root = (leftRoot + rightRoot) / 2;
        int leftPadding = Math.max(0, label.length() / 2 - root);
        int width = rightOffset + right.width() + leftPadding;
        root += leftPadding;
        leftRoot += leftPadding;
        rightRoot += leftPadding;
        int labelEnd = root - label.length() / 2 + label.length();
        width = Math.max(width, labelEnd);

        List<String> lines = new ArrayList<>();
        lines.add(place(label, width, root - label.length() / 2));
        lines.add(place("|", width, root));
        char[] branches = blankLine(width);
        Arrays.fill(branches, leftRoot, rightRoot + 1, '-');
        lines.add(new String(branches));

        char[] childBranches = blankLine(width);
        childBranches[leftRoot] = '|';
        childBranches[rightRoot] = '|';
        lines.add(new String(childBranches));

        int childOffset = lines.size();
        appendPadded(lines, left.lines(), leftPadding, width);
        appendPaddedAt(lines, right.lines(), rightOffset + leftPadding, width, childOffset);
        return new TreeLayout(lines, root, width);
    }

    private static void appendPadded(List<String> output, List<String> lines, int offset, int width) {
        appendPaddedAt(output, lines, offset, width, output.size());
    }

    private static void appendPaddedAt(
            List<String> output, List<String> lines, int offset, int width, int startRow) {
        for (int row = 0; row < lines.size(); row++) {
            while (output.size() <= startRow + row) {
                output.add("");
            }
            char[] combined = output.get(startRow + row).length() == width
                    ? output.get(startRow + row).toCharArray()
                    : blankLine(width);
            String childLine = lines.get(row);
            for (int column = 0; column < childLine.length(); column++) {
                char character = childLine.charAt(column);
                if (character != ' ') {
                    combined[offset + column] = character;
                }
            }
            output.set(startRow + row, new String(combined));
        }
    }

    private static String place(String value, int width, int offset) {
        char[] line = blankLine(width);
        value.getChars(0, value.length(), line, offset);
        return new String(line);
    }

    private static char[] blankLine(int width) {
        char[] line = new char[width];
        Arrays.fill(line, ' ');
        return line;
    }
}
