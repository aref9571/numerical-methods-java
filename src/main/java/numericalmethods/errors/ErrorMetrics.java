package numericalmethods.errors;

import numericalmethods.solver.InputValidation;

public final class ErrorMetrics {

    private ErrorMetrics() {
    }

    public static double absoluteError(double trueValue, double approximation) {
        InputValidation.requireFinite("True value", trueValue);
        InputValidation.requireFinite("Approximation", approximation);
        return requireRepresentable("Absolute error", Math.abs(trueValue - approximation));
    }

    public static double relativeError(double trueValue, double approximation) {
        double absoluteError = absoluteError(trueValue, approximation);
        if (trueValue == 0) {
            throw new ArithmeticException(
                    "Relative error is undefined when the true value is 0; use the absolute error instead");
        }
        return requireRepresentable("Relative error", absoluteError / Math.abs(trueValue));
    }

    public static double percentageError(double trueValue, double approximation) {
        double relativeError = relativeError(trueValue, approximation);
        return requireRepresentable("Percentage error", relativeError * 100);
    }

    private static double requireRepresentable(String metricName, double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(metricName + " is too large to represent as a finite number");
        }
        return value;
    }
}
