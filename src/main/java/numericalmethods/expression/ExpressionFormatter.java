package numericalmethods.expression;

public final class ExpressionFormatter {

    private static final double LARGEST_EXACT_WHOLE_NUMBER = 1e15;

    private ExpressionFormatter() {
    }

    public static String format(Expression expression) {
        return switch (expression) {
            case NumberExpression number -> formatNumber(number.value());
            case ConstantExpression constant -> constant.name();
            case VariableExpression variable -> variable.name();
            case NegationExpression negation -> "-" + formatOperand(negation.operand());
            case BinaryExpression binary -> formatOperand(binary.left()) + spacedSymbol(binary.operator())
                    + formatOperand(binary.right());
            case FunctionExpression function ->
                    function.function().functionName() + "(" + format(function.argument()) + ")";
        };
    }

    private static String formatOperand(Expression operand) {
        boolean needsParentheses = operand instanceof BinaryExpression
                || operand instanceof NegationExpression
                || operand instanceof NumberExpression number && number.value() < 0;
        String text = format(operand);
        return needsParentheses ? "(" + text + ")" : text;
    }

    private static String spacedSymbol(BinaryOperator operator) {
        return operator == BinaryOperator.POWER ? operator.symbol() : " " + operator.symbol() + " ";
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < LARGEST_EXACT_WHOLE_NUMBER) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
