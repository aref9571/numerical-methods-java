package numericalmethods.taylor;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import numericalmethods.errors.ErrorMetrics;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionEvaluationException;

public record TaylorApproximation(
        TaylorPolynomial polynomial,
        double target,
        double approximation,
        OptionalDouble exactValue,
        OptionalDouble absoluteError,
        OptionalDouble relativeError) {

    public TaylorApproximation {
        Objects.requireNonNull(polynomial, "polynomial");
        Objects.requireNonNull(exactValue, "exactValue");
        Objects.requireNonNull(absoluteError, "absoluteError");
        Objects.requireNonNull(relativeError, "relativeError");
    }

    public static TaylorApproximation of(
            Expression function, String variable, double center, int order, double target) {
        TaylorPolynomial polynomial = TaylorPolynomial.of(function, variable, center, order);
        double approximation = polynomial.evaluate(target);

        OptionalDouble exactValue = exactValueAt(function, variable, target);
        if (exactValue.isEmpty()) {
            return new TaylorApproximation(polynomial, target, approximation,
                    OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());
        }
        double exact = exactValue.getAsDouble();
        OptionalDouble relativeError = exact == 0
                ? OptionalDouble.empty()
                : OptionalDouble.of(ErrorMetrics.relativeError(exact, approximation));
        return new TaylorApproximation(polynomial, target, approximation, exactValue,
                OptionalDouble.of(ErrorMetrics.absoluteError(exact, approximation)), relativeError);
    }

    private static OptionalDouble exactValueAt(Expression function, String variable, double target) {
        try {
            return OptionalDouble.of(function.evaluate(Map.of(variable, target)));
        } catch (ExpressionEvaluationException undefined) {
            return OptionalDouble.empty();
        }
    }
}
