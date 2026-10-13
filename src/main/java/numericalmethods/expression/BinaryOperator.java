package numericalmethods.expression;

public enum BinaryOperator {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    POWER("^");

    private final String symbol;

    BinaryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public double apply(double left, double right) {
        double result = switch (this) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> divide(left, right);
            case POWER -> Math.pow(left, right);
        };
        if (!Double.isFinite(result)) {
            throw new ExpressionEvaluationException(
                    "Result of " + left + " " + symbol + " " + right + " is not a finite number");
        }
        return result;
    }

    private static double divide(double dividend, double divisor) {
        if (divisor == 0.0) {
            throw new ExpressionEvaluationException("Division by zero");
        }
        return dividend / divisor;
    }
}
