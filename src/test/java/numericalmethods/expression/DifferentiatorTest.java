package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DifferentiatorTest {

    private static Expression derivative(String input) {
        return Differentiator.differentiate(ExpressionParser.parse(input), "x");
    }

    private static double derivativeAt(String input, double x) {
        return derivative(input).evaluate(Map.of("x", x));
    }

    private static double centralDifference(String input, double x) {
        Expression expression = ExpressionParser.parse(input);
        double h = 1e-5 * Math.max(1.0, Math.abs(x));
        return (expression.evaluate(Map.of("x", x + h)) - expression.evaluate(Map.of("x", x - h))) / (2 * h);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "5 | 0",
            "pi | 0",
            "x | 1",
            "y | 0",
            "-x | -1",
            "3*x | 3",
            "x^2 | 2 * x",
            "x^3 | 3 * (x^2)",
            "x^3 - x - 2 | (3 * (x^2)) - 1",
            "sin(x) | cos(x)",
            "cos(x) | -sin(x)",
            "e^x | e^x",
            "exp(x) | exp(x)",
            "exp(2*x) | exp(2 * x) * 2",
            "ln(x) | 1 / x",
            "2^x | (2^x) * ln(2)",
            "sin(y) * x | sin(y)"
    })
    void producesSimplifiedDerivatives(String input, String expected) {
        assertEquals(expected, ExpressionFormatter.format(derivative(input)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x^3 - x - 2 | 2 | 11",
            "x*sin(x) | 0 | 0",
            "x/(1+x) | 1 | 0.25",
            "1/x | -2 | -0.25",
            "x^x | 2 | 6.772588722239781",
            "x^0.5 | 4 | 0.25",
            "tan(x) | 0 | 1",
            "asin(x) | 0.5 | 1.1547005383792515",
            "acos(x) | 0.5 | -1.1547005383792515",
            "atan(x) | 1 | 0.5",
            "log10(x) | 10 | 0.04342944819032518",
            "sqrt(x) | 9 | 0.16666666666666666",
            "abs(x) | -2 | -1",
            "abs(x) | 3 | 1",
            "sin(x^2) | 1 | 1.0806046117362795",
            "exp(sin(x)) | 0 | 1",
            "ln(cos(x)) | 0.5 | -0.5463024898437905",
            "-x^2 | 3 | -6"
    })
    void derivativeHasKnownValue(String input, double x, double expected) {
        assertEquals(expected, derivativeAt(input, x), 1e-12 * Math.max(1.0, Math.abs(expected)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x^3 - x - 2 | 1.5",
            "exp(-x^2/2) / sqrt(2*pi) | 0.7",
            "sin(x)*cos(x)/(1+x^2) | -1.3",
            "x^x | 1.7",
            "2^(x^2) | 0.4",
            "ln(x^2 + 1) * atan(x) | 2.2",
            "sqrt(x)^3 - log10(x) | 5",
            "e^(sin(x)) - x/abs(x - 4) | 2.5",
            "asin(x/2) + acos(x/3) | 0.9",
            "tan(x)^2 | 1.1",
            "1e-3 * x^4 - 250 * x | 12"
    })
    void derivativeAgreesWithFiniteDifference(String input, double x) {
        double expected = centralDifference(input, x);

        assertEquals(expected, derivativeAt(input, x), 1e-6 * Math.max(1.0, Math.abs(expected)));
    }

    @Test
    void repeatedDifferentiationWorks() {
        Expression expression = ExpressionParser.parse("sin(x)");
        for (int order = 0; order < 4; order++) {
            expression = Differentiator.differentiate(expression, "x");
        }

        assertEquals(Math.sin(0.7), expression.evaluate(Map.of("x", 0.7)), 1e-12);
    }

    @Test
    void partialDerivativeTreatsOtherVariablesAsConstants() {
        Expression derivative = Differentiator.differentiate(ExpressionParser.parse("x^2*y + y^3"), "y");

        assertEquals(4.0 + 27.0, derivative.evaluate(Map.of("x", 2.0, "y", 3.0)), 1e-12);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "abs(x) | 0",
            "sqrt(x) | 0",
            "asin(x) | 1",
            "acos(x) | -1",
            "x^0.5 | 0",
            "x^(1/3) | -8"
    })
    void nonDifferentiablePointsFailInsteadOfReturningAValue(String input, double x) {
        Expression derivative = derivative(input);

        assertThrows(ExpressionEvaluationException.class, () -> derivative.evaluate(Map.of("x", x)));
    }

    @Test
    void derivativeThatGrowsTooLargeIsRejected() {
        String input = "x" + "*x".repeat(399);

        ExpressionEvaluationException exception =
                assertThrows(ExpressionEvaluationException.class, () -> derivative(input));

        assertTrue(exception.getMessage().startsWith("The derivative is too large"), exception.getMessage());
    }

    @Test
    void moderateProductsStayWithinTheSizeLimit() {
        String input = "x" + "*x".repeat(19);

        assertEquals(20.0, derivativeAt(input, 1.0), 1e-12);
    }

    @Test
    void derivativeTextParsesBackToAnEquivalentExpression() {
        Expression derivative = derivative("x^x * exp(-x) / (1 + ln(x))");
        Expression reparsed = ExpressionParser.parse(ExpressionFormatter.format(derivative));

        assertEquals(derivative.evaluate(Map.of("x", 1.3)), reparsed.evaluate(Map.of("x", 1.3)), 1e-12);
    }

    @Test
    void dependsOnFindsVariablesAnywhere() {
        assertTrue(Differentiator.dependsOn(ExpressionParser.parse("2 + sin(3 * -x)"), "x"));
        assertFalse(Differentiator.dependsOn(ExpressionParser.parse("2 + sin(3 * -y) + pi"), "x"));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> Differentiator.differentiate(null, "x"));
        assertThrows(NullPointerException.class,
                () -> Differentiator.differentiate(ExpressionParser.parse("x"), null));
    }
}
