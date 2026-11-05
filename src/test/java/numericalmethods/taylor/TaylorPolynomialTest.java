package numericalmethods.taylor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import numericalmethods.expression.ExpressionEvaluationException;
import numericalmethods.expression.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaylorPolynomialTest {

    private static TaylorPolynomial taylor(String function, double center, int order) {
        return TaylorPolynomial.of(ExpressionParser.parse(function), "x", center, order);
    }

    private static void assertCoefficients(List<Double> expected, TaylorPolynomial polynomial) {
        assertEquals(expected.size(), polynomial.coefficients().size());
        for (int k = 0; k < expected.size(); k++) {
            assertEquals(expected.get(k), polynomial.coefficients().get(k), 1e-12, "coefficient " + k);
        }
    }

    @Test
    void constantFunctionHasOnlyAConstantTerm() {
        TaylorPolynomial polynomial = taylor("5", 2.0, 3);

        assertCoefficients(List.of(5.0, 0.0, 0.0, 0.0), polynomial);
        assertEquals(5.0, polynomial.evaluate(-100.0), 1e-12);
    }

    @Test
    void linearFunctionIsReproducedExactly() {
        TaylorPolynomial polynomial = taylor("2*x + 1", 3.0, 1);

        assertCoefficients(List.of(7.0, 2.0), polynomial);
        assertEquals(-19.0, polynomial.evaluate(-10.0), 1e-12);
    }

    @Test
    void quadraticAboutOneHasKnownCoefficients() {
        TaylorPolynomial polynomial = taylor("x^2", 1.0, 4);

        assertCoefficients(List.of(1.0, 2.0, 1.0, 0.0, 0.0), polynomial);
        assertEquals(6.25, polynomial.evaluate(2.5), 1e-12);
    }

    @Test
    void exponentialAboutZeroHasInverseFactorialCoefficients() {
        TaylorPolynomial polynomial = taylor("exp(x)", 0.0, 10);

        List<Double> expected = new ArrayList<>();
        double factorial = 1;
        for (int k = 0; k <= 10; k++) {
            factorial *= k == 0 ? 1 : k;
            expected.add(1 / factorial);
        }
        assertCoefficients(expected, polynomial);
    }

    @Test
    void sineAboutZeroHasOnlyOddTerms() {
        TaylorPolynomial polynomial = taylor("sin(x)", 0.0, 5);

        assertCoefficients(List.of(0.0, 1.0, 0.0, -1.0 / 6, 0.0, 1.0 / 120), polynomial);
    }

    @Test
    void cosineAboutPiHasKnownCoefficients() {
        TaylorPolynomial polynomial = taylor("cos(x)", Math.PI, 4);

        assertCoefficients(List.of(-1.0, 0.0, 0.5, 0.0, -1.0 / 24), polynomial);
    }

    @ParameterizedTest
    @CsvSource({
            "exp(x), 0, 10, 1, 2.718281828459045, 1e-7",
            "exp(x), 0, 20, -2, 0.1353352832366127, 1e-12",
            "sin(x), 0, 7, 0.5, 0.479425538604203, 1e-8",
            "cos(x), 0, 8, 1, 0.5403023058681398, 1e-6",
            "ln(x), 1, 6, 1.2, 0.1823215567939546, 1e-5",
            "sqrt(x), 4, 3, 4.41, 2.1, 1e-5",
            "1/(1-x), 0, 7, 0.1, 1.1111111111111112, 1e-7"
    })
    void approximationIsCloseNearTheCenter(
            String function, double center, int order, double target, double expected, double tolerance) {
        assertEquals(expected, taylor(function, center, order).evaluate(target), tolerance);
    }

    @Test
    void higherOrderGivesASmallerErrorForExp() {
        double exact = Math.exp(1);
        double previousError = Double.POSITIVE_INFINITY;
        for (int order = 1; order <= 12; order++) {
            double error = Math.abs(exact - taylor("exp(x)", 0.0, order).evaluate(1.0));
            assertTrue(error < previousError, "order " + order);
            previousError = error;
        }
    }

    @Test
    void orderZeroIsTheFunctionValue() {
        TaylorPolynomial polynomial = taylor("x^3 - x - 2", 2.0, 0);

        assertEquals(0, polynomial.order());
        assertEquals(4.0, polynomial.evaluate(17.0), 1e-12);
    }

    @Test
    void maximumOrderIsSupportedForSimpleFunctions() {
        TaylorPolynomial polynomial = taylor("sin(x)", 0.0, TaylorPolynomial.MAX_ORDER);

        assertEquals(20, polynomial.order());
        assertEquals(Math.sin(2.0), polynomial.evaluate(2.0), 1e-10);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, TaylorPolynomial.MAX_ORDER + 1})
    void orderOutsideTheAllowedRangeIsRejected(int order) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> taylor("exp(x)", 0.0, order));

        assertTrue(exception.getMessage().startsWith("Order must be between 0 and 20"), exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
    void nonFiniteCenterIsRejected(double center) {
        assertThrows(IllegalArgumentException.class, () -> taylor("exp(x)", center, 3));
    }

    @Test
    void nonFiniteTargetIsRejected() {
        TaylorPolynomial polynomial = taylor("exp(x)", 0.0, 3);

        assertThrows(IllegalArgumentException.class, () -> polynomial.evaluate(Double.NaN));
    }

    @ParameterizedTest
    @CsvSource({
            "sqrt(x), 0, 2, Derivative 1 is undefined at x = 0.0",
            "abs(x), 0, 1, Derivative 1 is undefined at x = 0.0",
            "ln(x), 0, 3, The function is undefined at x = 0.0",
            "ln(x), -1, 3, The function is undefined at x = -1.0",
            "x + y, 0, 1, The function is undefined at x = 0.0"
    })
    void undefinedFunctionOrDerivativeAtTheCenterIsReported(
            String function, double center, int order, String expectedStart) {
        ExpressionEvaluationException exception =
                assertThrows(ExpressionEvaluationException.class, () -> taylor(function, center, order));

        assertTrue(exception.getMessage().startsWith(expectedStart), exception.getMessage());
    }

    @Test
    void derivativesThatGrowTooLargeAreReported() {
        ExpressionEvaluationException exception =
                assertThrows(ExpressionEvaluationException.class, () -> taylor("1/(1+x^2)", 0.0, 12));

        assertTrue(exception.getMessage().startsWith("The derivative is too large"), exception.getMessage());
    }

    @Test
    void polynomialValueThatOverflowsIsReported() {
        TaylorPolynomial polynomial = taylor("exp(x)", 0.0, 20);

        ArithmeticException exception = assertThrows(ArithmeticException.class, () -> polynomial.evaluate(1e300));

        assertTrue(exception.getMessage().contains("not a finite number"), exception.getMessage());
    }

    @Test
    void coefficientsCannotBeModified() {
        TaylorPolynomial polynomial = taylor("exp(x)", 0.0, 2);

        assertThrows(UnsupportedOperationException.class, () -> polynomial.coefficients().set(0, 9.0));
    }

    @Test
    void emptyOrNonFiniteCoefficientsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TaylorPolynomial(0.0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TaylorPolynomial(0.0, List.of(1.0, Double.NaN)));
    }
}
