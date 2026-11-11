package com.compiler.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Renders expression and statement trees as top-down, textbook-style trees.
 * @author Github Copilot
 */
public final class PrettyPrinter {
    private PrettyPrinter() { }

    private record TreeLayout(List<String> lines, int root, int width) { }

    public static String format(Parser.Expr expression) {
        return String.join(System.lineSeparator(), layout(expression).lines());
    }

    public static String format(Parser.Stmt statement) {
        return String.join(System.lineSeparator(), layout(statement).lines());
    }

    private static TreeLayout layout(Parser.Stmt statement) {
        if (statement instanceof Parser.Stmt.Block block) {
            List<TreeLayout> children = block.statements().stream()
                    .map(PrettyPrinter::layout)
                    .toList();
            return layoutNode("Block", children);
        }

        if (statement instanceof Parser.Stmt.AssignmentStatement assignment) {
            return layoutNode(
                    "AssignmentStatement",
                    List.of(layout(assignment.assignment()))
            );
        }

        if (statement instanceof Parser.Stmt.IfStatement ifStatement) {
            List<TreeLayout> children = new ArrayList<>();
            children.add(layoutNode("Condition", List.of(layout(ifStatement.condition()))));
            children.add(layoutNode("Then", List.of(layout(ifStatement.ifBody()))));
            if (ifStatement.elseBody() != null) {
                children.add(layoutNode("Else", List.of(layout(ifStatement.elseBody()))));
            }
            return layoutNode("IfStatement", children);
        }

        throw new IllegalStateException("Unknown statement type: " + statement.getClass());
    }

    private static TreeLayout layout(Parser.Expr expression) {
        if (expression instanceof Parser.Expr.Literal literal) {
            String label = String.valueOf(literal.value());
            return new TreeLayout(List.of(label), label.length() / 2, label.length());
        }

        if (expression instanceof Parser.Expr.Variable variable) {
            String label = variable.name().lexeme();
            return new TreeLayout(List.of(label), label.length() / 2, label.length());
        }

        if (expression instanceof Parser.Expr.Grouping grouping) {
            TreeLayout child = layout(grouping.expression());
            int childOffset = Math.max(0, "group".length() / 2 - child.root());
            int root = childOffset + child.root();
            int width = Math.max("group".length(), childOffset + child.width());
            List<String> lines = new ArrayList<>();
            lines.add(place("group", width, root - "group".length() / 2));
            lines.add(place("|", width, root));
            appendPadded(lines, child.lines(), childOffset, width);
            return new TreeLayout(lines, root, width);
        }

        if (expression instanceof Parser.Expr.Assign assign) {
            return layoutBinary(assign.Assignop().lexeme(), assign.target(), assign.value());
        }

        if (expression instanceof Parser.Expr.Binary binary) {
            return layoutBinary(binary.operator().lexeme(), binary.left(), binary.right());
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

        char[] branches = blankLine(width);
        int leftmostRoot = firstRoot + leftPadding;
        int rightmostRoot = lastRoot + leftPadding;
        Arrays.fill(branches, Math.min(root, leftmostRoot), Math.max(root, rightmostRoot) + 1, '-');
        branches[root] = '+';
        for (int i = 0; i < children.size(); i++) {
            branches[offsets[i] + children.get(i).root() + leftPadding] = '+';
        }
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

    private static TreeLayout layoutBinary(String label, Parser.Expr leftExpression, Parser.Expr rightExpression) {
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
        branches[leftRoot] = '+';
        branches[rightRoot] = '+';
        branches[root] = '+';
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
