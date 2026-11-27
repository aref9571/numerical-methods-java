package numericalmethods.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionParser;
import numericalmethods.expression.ExpressionVariables;

public final class Inputs {

    private Inputs() {
    }

    public static double number(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("A number is required");
        }
        Expression expression = ExpressionParser.parse(text.trim());
        Set<String> variables = ExpressionVariables.of(expression);
        if (!variables.isEmpty()) {
            throw new IllegalArgumentException("'" + text.trim() + "' must be a number or a constant expression"
                    + " such as pi/6, but it uses " + variables);
        }
        return expression.evaluate(Map.of());
    }

    public static int integer(String text) {
        double value = number(text);
        if (value != Math.rint(value) || Math.abs(value) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("'" + text.trim() + "' must be a whole number");
        }
        return (int) value;
    }

    public static String singleVariable(Expression expression, String fallback) {
        Set<String> variables = ExpressionVariables.of(expression);
        if (variables.isEmpty()) {
            return fallback;
        }
        if (variables.size() > 1) {
            throw new IllegalArgumentException("The function must use one variable, but it uses " + variables);
        }
        return variables.iterator().next();
    }

    public static double[] vector(String text) {
        String inner = stripBrackets(text.trim(), '[', ']');
        List<String> parts = splitNumbers(inner);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("The vector is empty");
        }
        double[] values = new double[parts.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = number(parts.get(i));
        }
        return values;
    }

    public static List<String> splitTopLevel(String text, char separator) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (c == separator && depth == 0) {
                parts.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(text.substring(start).trim());
        return parts;
    }

    private static List<String> splitNumbers(String text) {
        List<String> parts = new ArrayList<>();
        String separators = text.contains(",") ? "," : "\\s+";
        for (String part : text.trim().split(separators)) {
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return parts;
    }

    private static String stripBrackets(String text, char open, char close) {
        if (text.length() >= 2 && text.charAt(0) == open && text.charAt(text.length() - 1) == close) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
