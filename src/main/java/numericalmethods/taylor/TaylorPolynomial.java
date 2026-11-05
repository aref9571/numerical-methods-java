package numericalmethods.taylor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import numericalmethods.expression.Differentiator;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionEvaluationException;
import numericalmethods.solver.InputValidation;

public record TaylorPolynomial(double center, List<Double> coefficients) {

    public static final int MAX_ORDER = 20;

    public TaylorPolynomial {
        InputValidation.requireFinite("Center", center);
        coefficients = List.copyOf(coefficients);
        if (coefficients.isEmpty()) {
            throw new IllegalArgumentException("A Taylor polynomial needs at least one coefficient");
        }
        for (double coefficient : coefficients) {
            InputValidation.requireFinite("Coefficient", coefficient);
        }
    }

    public static TaylorPolynomial of(Expression function, String variable, double center, int order) {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(variable, "variable");
        InputValidation.requireFinite("Center", center);
        InputValidation.requireInRange("Order", order, 0, MAX_ORDER);

        Map<String, Double> atCenter = Map.of(variable, center);
        List<Double> coefficients = new ArrayList<>();
        Expression derivative = function;
        double factorial = 1;
        for (int k = 0; k <= order; k++) {
            if (k > 0) {
                derivative = Differentiator.differentiate(derivative, variable);
                factorial *= k;
            }
            coefficients.add(derivativeValue(derivative, atCenter, k, variable, center) / factorial);
        }
        return new TaylorPolynomial(center, coefficients);
    }

    private static double derivativeValue(
            Expression derivative, Map<String, Double> atCenter, int k, String variable, double center) {
        try {
            return derivative.evaluate(atCenter);
        } catch (ExpressionEvaluationException exception) {
            String what = k == 0 ? "The function" : "Derivative " + k;
            throw new ExpressionEvaluationException(what + " is undefined at " + variable + " = " + center
                    + " (" + exception.getMessage() + ")");
        }
    }

    public int order() {
        return coefficients.size() - 1;
    }

    public double evaluate(double x) {
        InputValidation.requireFinite("Target point", x);
        double offset = x - center;
        double value = 0;
        for (int k = coefficients.size() - 1; k >= 0; k--) {
            value = value * offset + coefficients.get(k);
        }
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("The Taylor polynomial value at " + x + " is not a finite number");
        }
        return value;
    }
}
