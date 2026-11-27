package numericalmethods.expression;

import java.util.Set;
import java.util.TreeSet;

public final class ExpressionVariables {

    private ExpressionVariables() {
    }

    public static Set<String> of(Expression expression) {
        Set<String> names = new TreeSet<>();
        collect(expression, names);
        return names;
    }

    private static void collect(Expression expression, Set<String> names) {
        switch (expression) {
            case NumberExpression number -> {
            }
            case ConstantExpression constant -> {
            }
            case VariableExpression variable -> names.add(variable.name());
            case NegationExpression negation -> collect(negation.operand(), names);
            case BinaryExpression binary -> {
                collect(binary.left(), names);
                collect(binary.right(), names);
            }
            case FunctionExpression function -> collect(function.argument(), names);
        }
    }
}
