package numericalmethods.expression;

import java.util.Objects;

public final class Differentiator {

    public static final int MAX_DERIVATIVE_SIZE = 10_000;

    private static final Expression ZERO = new NumberExpression(0);
    private static final Expression ONE = new NumberExpression(1);

    private final String variable;

    private Differentiator(String variable) {
        this.variable = variable;
    }

    public static Expression differentiate(Expression expression, String variable) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(variable, "variable");
        Expression derivative = new Differentiator(variable).derivativeOf(expression);
        if (sizeOf(derivative) > MAX_DERIVATIVE_SIZE) {
            throw new ExpressionEvaluationException(
                    "The derivative is too large: more than " + MAX_DERIVATIVE_SIZE + " terms");
        }
        return derivative;
    }

    public static boolean dependsOn(Expression expression, String variable) {
        return switch (expression) {
            case NumberExpression number -> false;
            case ConstantExpression constant -> false;
            case VariableExpression variableExpression -> variableExpression.name().equals(variable);
            case NegationExpression negation -> dependsOn(negation.operand(), variable);
            case BinaryExpression binary -> dependsOn(binary.left(), variable) || dependsOn(binary.right(), variable);
            case FunctionExpression function -> dependsOn(function.argument(), variable);
        };
    }

    static int sizeOf(Expression expression) {
        return switch (expression) {
            case NegationExpression negation -> 1 + sizeOf(negation.operand());
            case BinaryExpression binary -> 1 + sizeOf(binary.left()) + sizeOf(binary.right());
            case FunctionExpression function -> 1 + sizeOf(function.argument());
            default -> 1;
        };
    }

    private Expression derivativeOf(Expression expression) {
        if (!dependsOn(expression, variable)) {
            return ZERO;
        }
        return switch (expression) {
            case NegationExpression negation -> negate(derivativeOf(negation.operand()));
            case BinaryExpression binary -> derivativeOfBinary(binary);
            case FunctionExpression function ->
                    multiply(derivativeOfFunction(function.function(), function.argument()),
                            derivativeOf(function.argument()));
            default -> ONE;
        };
    }

    private Expression derivativeOfBinary(BinaryExpression binary) {
        Expression u = binary.left();
        Expression v = binary.right();
        return switch (binary.operator()) {
            case ADD -> add(derivativeOf(u), derivativeOf(v));
            case SUBTRACT -> subtract(derivativeOf(u), derivativeOf(v));
            case MULTIPLY -> add(multiply(derivativeOf(u), v), multiply(u, derivativeOf(v)));
            case DIVIDE -> divide(
                    subtract(multiply(derivativeOf(u), v), multiply(u, derivativeOf(v))),
                    power(v, new NumberExpression(2)));
            case POWER -> derivativeOfPower(u, v);
        };
    }

    private Expression derivativeOfPower(Expression base, Expression exponent) {
        if (!dependsOn(exponent, variable)) {
            return multiply(multiply(exponent, power(base, subtract(exponent, ONE))), derivativeOf(base));
        }
        Expression basePower = new BinaryExpression(BinaryOperator.POWER, base, exponent);
        if (base instanceof ConstantExpression constant && constant.name().equals("e")) {
            return multiply(basePower, derivativeOf(exponent));
        }
        Expression logBase = new FunctionExpression(MathFunction.LN, base);
        return multiply(basePower,
                add(multiply(derivativeOf(exponent), logBase), divide(multiply(exponent, derivativeOf(base)), base)));
    }

    private static Expression derivativeOfFunction(MathFunction function, Expression u) {
        return switch (function) {
            case SIN -> call(MathFunction.COS, u);
            case COS -> negate(call(MathFunction.SIN, u));
            case TAN -> divide(ONE, power(call(MathFunction.COS, u), new NumberExpression(2)));
            case ASIN -> divide(ONE, call(MathFunction.SQRT, subtract(ONE, power(u, new NumberExpression(2)))));
            case ACOS -> negate(divide(ONE,
                    call(MathFunction.SQRT, subtract(ONE, power(u, new NumberExpression(2))))));
            case ATAN -> divide(ONE, add(ONE, power(u, new NumberExpression(2))));
            case EXP -> call(MathFunction.EXP, u);
            case LN -> divide(ONE, u);
            case LOG10 -> divide(ONE, multiply(u, call(MathFunction.LN, new NumberExpression(10))));
            case SQRT -> divide(ONE, multiply(new NumberExpression(2), call(MathFunction.SQRT, u)));
            case ABS -> divide(u, call(MathFunction.ABS, u));
        };
    }

    private static Expression call(MathFunction function, Expression argument) {
        return new FunctionExpression(function, argument);
    }

    private static Expression negate(Expression operand) {
        if (operand instanceof NumberExpression number) {
            return new NumberExpression(-number.value());
        }
        if (operand instanceof NegationExpression negation) {
            return negation.operand();
        }
        return new NegationExpression(operand);
    }

    private static Expression add(Expression left, Expression right) {
        if (isNumber(left, 0)) {
            return right;
        }
        if (isNumber(right, 0)) {
            return left;
        }
        return foldOrBuild(BinaryOperator.ADD, left, right);
    }

    private static Expression subtract(Expression left, Expression right) {
        if (isNumber(right, 0)) {
            return left;
        }
        if (isNumber(left, 0)) {
            return negate(right);
        }
        return foldOrBuild(BinaryOperator.SUBTRACT, left, right);
    }

    private static Expression multiply(Expression left, Expression right) {
        if (isNumber(left, 0) || isNumber(right, 0)) {
            return ZERO;
        }
        if (isNumber(left, 1)) {
            return right;
        }
        if (isNumber(right, 1)) {
            return left;
        }
        return foldOrBuild(BinaryOperator.MULTIPLY, left, right);
    }

    private static Expression divide(Expression dividend, Expression divisor) {
        if (isNumber(divisor, 1)) {
            return dividend;
        }
        return foldOrBuild(BinaryOperator.DIVIDE, dividend, divisor);
    }

    private static Expression power(Expression base, Expression exponent) {
        if (isNumber(exponent, 1)) {
            return base;
        }
        if (isNumber(exponent, 0)) {
            return ONE;
        }
        return foldOrBuild(BinaryOperator.POWER, base, exponent);
    }

    private static Expression foldOrBuild(BinaryOperator operator, Expression left, Expression right) {
        if (left instanceof NumberExpression leftNumber && right instanceof NumberExpression rightNumber) {
            try {
                return new NumberExpression(operator.apply(leftNumber.value(), rightNumber.value()));
            } catch (ExpressionEvaluationException notFoldable) {
                return new BinaryExpression(operator, left, right);
            }
        }
        return new BinaryExpression(operator, left, right);
    }

    private static boolean isNumber(Expression expression, double value) {
        return expression instanceof NumberExpression number && number.value() == value;
    }
}
