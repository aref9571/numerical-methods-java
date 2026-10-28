package numericalmethods.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ErrorMetricsTest {

    private static final double TOLERANCE = 1e-12;

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.0, -7.25, 1e300, -1e-300})
    void exactAgreementHasZeroAbsoluteError(double value) {
        assertEquals(0.0, ErrorMetrics.absoluteError(value, value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, -7.25, 1e300, -1e-300})
    void exactAgreementHasZeroRelativeAndPercentageError(double value) {
        assertEquals(0.0, ErrorMetrics.relativeError(value, value));
        assertEquals(0.0, ErrorMetrics.percentageError(value, value));
    }

    @ParameterizedTest
    @CsvSource({
            "3.141592653589793, 3.14, 0.001592653589793, 5.069573828972128e-4, 0.05069573828972128",
            "2.0, 2.5, 0.5, 0.25, 25.0",
            "100.0, 99.0, 1.0, 0.01, 1.0",
            "-2.0, -2.1, 0.1, 0.05, 5.0",
            "-4.0, 4.0, 8.0, 2.0, 200.0",
            "0.001, 0.0011, 1e-4, 0.1, 10.0"
    })
    void approximateAgreement(double trueValue, double approximation,
            double expectedAbsolute, double expectedRelative, double expectedPercentage) {
        assertEquals(expectedAbsolute, ErrorMetrics.absoluteError(trueValue, approximation), TOLERANCE);
        assertEquals(expectedRelative, ErrorMetrics.relativeError(trueValue, approximation), TOLERANCE);
        assertEquals(expectedPercentage, ErrorMetrics.percentageError(trueValue, approximation), 1e-10);
    }

    @Test
    void absoluteErrorIsSymmetric() {
        assertEquals(ErrorMetrics.absoluteError(1.5, 4.0), ErrorMetrics.absoluteError(4.0, 1.5));
    }

    @Test
    void relativeErrorIsMeasuredAgainstTheTrueValue() {
        assertEquals(0.5, ErrorMetrics.relativeError(2.0, 3.0), TOLERANCE);
        assertEquals(1.0 / 3.0, ErrorMetrics.relativeError(3.0, 2.0), TOLERANCE);
    }

    @Test
    void absoluteErrorIsDefinedForAZeroTrueValue() {
        assertEquals(0.25, ErrorMetrics.absoluteError(0.0, -0.25), TOLERANCE);
    }

    @ParameterizedTest
    @CsvSource({"0.0, 0.5", "0.0, 0.0", "-0.0, 1e-9"})
    void relativeAndPercentageErrorAreUndefinedForAZeroTrueValue(double trueValue, double approximation) {
        ArithmeticException relative = assertThrows(ArithmeticException.class,
                () -> ErrorMetrics.relativeError(trueValue, approximation));
        ArithmeticException percentage = assertThrows(ArithmeticException.class,
                () -> ErrorMetrics.percentageError(trueValue, approximation));

        assertTrue(relative.getMessage().startsWith("Relative error is undefined when the true value is 0"),
                relative.getMessage());
        assertEquals(relative.getMessage(), percentage.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteTrueValueIsRejected(double trueValue) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ErrorMetrics.absoluteError(trueValue, 1.0));

        assertTrue(exception.getMessage().startsWith("True value must be a finite number"), exception.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ErrorMetrics.relativeError(trueValue, 1.0));
        assertThrows(IllegalArgumentException.class, () -> ErrorMetrics.percentageError(trueValue, 1.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteApproximationIsRejected(double approximation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ErrorMetrics.absoluteError(1.0, approximation));

        assertTrue(exception.getMessage().startsWith("Approximation must be a finite number"),
                exception.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ErrorMetrics.relativeError(1.0, approximation));
        assertThrows(IllegalArgumentException.class, () -> ErrorMetrics.percentageError(1.0, approximation));
    }

    @Test
    void absoluteErrorThatOverflowsIsReported() {
        ArithmeticException exception = assertThrows(ArithmeticException.class,
                () -> ErrorMetrics.absoluteError(1e308, -1e308));

        assertTrue(exception.getMessage().startsWith("Absolute error is too large"), exception.getMessage());
    }

    @Test
    void relativeErrorThatOverflowsIsReported() {
        ArithmeticException exception = assertThrows(ArithmeticException.class,
                () -> ErrorMetrics.relativeError(1e-300, 1e10));

        assertTrue(exception.getMessage().startsWith("Relative error is too large"), exception.getMessage());
    }

    @Test
    void percentageErrorThatOverflowsIsReported() {
        ArithmeticException exception = assertThrows(ArithmeticException.class,
                () -> ErrorMetrics.percentageError(1e-300, 1e7));

        assertTrue(exception.getMessage().startsWith("Percentage error is too large"), exception.getMessage());
    }
}
