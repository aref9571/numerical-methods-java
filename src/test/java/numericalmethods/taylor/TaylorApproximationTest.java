package numericalmethods.taylor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import numericalmethods.expression.ExpressionParser;
import org.junit.jupiter.api.Test;

class TaylorApproximationTest {

    private static TaylorApproximation approximate(String function, double center, int order, double target) {
        return TaylorApproximation.of(ExpressionParser.parse(function), "x", center, order, target);
    }

    @Test
    void reportsExactValueAndObservedErrors() {
        TaylorApproximation result = approximate("exp(x)", 0.0, 4, 1.0);

        double exact = Math.exp(1);
        double approximation = 1 + 1 + 0.5 + 1.0 / 6 + 1.0 / 24;
        assertEquals(1.0, result.target());
        assertEquals(4, result.polynomial().order());
        assertEquals(approximation, result.approximation(), 1e-12);
        assertEquals(exact, result.exactValue().getAsDouble(), 1e-12);
        assertEquals(exact - approximation, result.absoluteError().getAsDouble(), 1e-12);
        assertEquals((exact - approximation) / exact, result.relativeError().getAsDouble(), 1e-12);
    }

    @Test
    void exactPolynomialHasZeroError() {
        TaylorApproximation result = approximate("x^3 - 2*x", 1.0, 3, -2.0);

        assertEquals(-4.0, result.approximation(), 1e-12);
        assertEquals(0.0, result.absoluteError().getAsDouble(), 1e-12);
    }

    @Test
    void relativeErrorIsLeftOutWhenTheExactValueIsZero() {
        TaylorApproximation result = approximate("x^2 - 4", 0.5, 1, 2.0);

        assertEquals(0.0, result.exactValue().getAsDouble());
        assertTrue(result.absoluteError().isPresent());
        assertTrue(result.relativeError().isEmpty());
    }

    @Test
    void errorsAreLeftOutWhenTheFunctionIsUndefinedAtTheTarget() {
        TaylorApproximation result = approximate("ln(x)", 1.0, 5, -0.5);

        assertTrue(Double.isFinite(result.approximation()));
        assertTrue(result.exactValue().isEmpty());
        assertTrue(result.absoluteError().isEmpty());
        assertTrue(result.relativeError().isEmpty());
    }

    @Test
    void farFromTheCenterTheObservedErrorCanBeLarge() {
        TaylorApproximation result = approximate("ln(x)", 1.0, 6, 3.0);

        assertTrue(result.absoluteError().getAsDouble() > 1.0, "error " + result.absoluteError());
    }
}
