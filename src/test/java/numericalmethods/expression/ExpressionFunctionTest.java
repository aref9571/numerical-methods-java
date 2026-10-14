package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ExpressionFunctionTest {

    private static final double TOLERANCE = 1e-12;

    private static double evaluate(String input) {
        return ExpressionParser.parse(input).evaluate(Map.of());
    }

    private static double evaluate(String input, Map<String, Double> variables) {
        return ExpressionParser.parse(input).evaluate(variables);
    }

    @Test
    void constantsHaveTheirMathematicalValues() {
        assertEquals(Math.PI, evaluate("pi"), TOLERANCE);
        assertEquals(Math.E, evaluate("e"), TOLERANCE);
        assertEquals(2 * Math.PI, evaluate("2*pi"), TOLERANCE);
        assertEquals(-Math.E, evaluate("-e"), TOLERANCE);
    }

    @Test
    void constantsDoNotNeedVariableValues() {
        assertEquals(Math.PI + Math.E, ExpressionParser.parse("pi + e").evaluate(Map.of()), TOLERANCE);
    }

    @Test
    void constantsCannotBeOverriddenByVariableMap() {
        assertEquals(Math.PI, evaluate("pi", Map.of("pi", 3.0)), TOLERANCE);
    }

    @ParameterizedTest
    @CsvSource({
            "'sin(0)', 0",
            "'sin(pi/2)', 1",
            "'cos(0)', 1",
            "'cos(pi)', -1",
            "'tan(pi/4)', 1",
            "'asin(1)', 1.5707963267948966",
            "'acos(1)', 0",
            "'atan(1)', 0.7853981633974483",
            "'exp(0)', 1",
            "'exp(1)', 2.718281828459045",
            "'ln(e)', 1",
            "'ln(1)', 0",
            "'log10(1000)', 3",
            "'sqrt(16)', 4",
            "'sqrt(0)', 0",
            "'abs(-3.5)', 3.5",
            "'abs(2)', 2"
    })
    void evaluatesEachFunction(String input, double expected) {
        assertEquals(expected, evaluate(input), TOLERANCE);
    }

    @ParameterizedTest
    @EnumSource(MathFunction.class)
    void everyFunctionCanBeParsedByName(MathFunction function) {
        Expression expression = ExpressionParser.parse(function.functionName() + "(x)");

        assertEquals(new FunctionExpression(function, new VariableExpression("x")), expression);
    }

    @ParameterizedTest
    @CsvSource({
            "'sqrt(abs(-16))', 4",
            "'exp(ln(5))', 5",
            "'sin(x)^2 + cos(x)^2', 1",
            "'2*sin(x)*cos(x) - sin(2*x)', 0",
            "'-sqrt(4)^2', -4",
            "'sqrt(sqrt(sqrt(256)))', 2",
            "'ln(exp(x))', 0.7"
    })
    void evaluatesNestedFunctions(String input, double expected) {
        assertEquals(expected, evaluate(input, Map.of("x", 0.7)), TOLERANCE);
    }

    @Test
    void functionBindsTighterThanExponentOnItsResult() {
        assertEquals(9.0, evaluate("abs(-3)^2"), TOLERANCE);
    }

    @Test
    void evaluatesTypicalOdeRightHandSide() {
        Expression expression = ExpressionParser.parse("y2 - t*sin(y1)");

        assertEquals(3.0 - 2.0 * Math.sin(1.0),
                expression.evaluate(Map.of("t", 2.0, "y1", 1.0, "y2", 3.0)), TOLERANCE);
    }

    @ParameterizedTest
    @CsvSource({
            "'sqrt(-1)'",
            "'ln(0)'",
            "'ln(-2)'",
            "'log10(0)'",
            "'asin(2)'",
            "'acos(-1.5)'",
            "'exp(1000)'",
            "'tan(pi/2)'",
            "'tan(-pi/2)'",
            "'tan(3*pi/2)'",
            "'tan(1001*pi/2)'"
    })
    void domainErrorsFailWithFunctionName(String input) {
        ExpressionEvaluationException exception =
                assertThrows(ExpressionEvaluationException.class, () -> evaluate(input));

        String functionName = input.substring(0, input.indexOf('('));
        assertTrue(exception.getMessage().startsWith(functionName + "("), exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "'tan(1.5707)', 10381.327285903",
            "'tan(pi/2 - 1e-8)', 1e8",
            "'tan(pi)', 0",
            "'tan(0)', 0"
    })
    void tangentNearButNotAtAPoleIsFinite(String input, double expected) {
        assertEquals(expected, evaluate(input), Math.abs(expected) * 1e-6 + TOLERANCE);
    }

    @Test
    void domainErrorDependsOnVariableValue() {
        Expression expression = ExpressionParser.parse("sqrt(x)");

        assertEquals(3.0, expression.evaluate(Map.of("x", 9.0)), TOLERANCE);
        assertThrows(ExpressionEvaluationException.class, () -> expression.evaluate(Map.of("x", -9.0)));
    }

    @Test
    void missingVariableInsideFunctionFails() {
        ExpressionEvaluationException exception =
                assertThrows(ExpressionEvaluationException.class, () -> evaluate("sin(y1) + y2", Map.of("y1", 0.0)));

        assertTrue(exception.getMessage().contains("'y2'"), exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "'foo(2)', 0, Unknown function 'foo'",
            "'Sin(x)', 0, Unknown function 'Sin'",
            "'x(2)', 0, Unknown function 'x'",
            "'pi(2)', 0, Unknown function 'pi'",
            "'sin()', 4, needs one argument",
            "'sin(1, 2)', 5, takes exactly one argument",
            "'sin(x', 5, is never closed",
            "'sin x', 0, must be followed by '('",
            "'2*sqrt', 2, must be followed by '('",
            "'1, 2', 1, Unexpected ','",
            "'(1, 2)', 2, is never closed"
    })
    void rejectsInvalidFunctionSyntax(String input, int expectedPosition, String expectedMessagePart) {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(input));

        assertEquals(expectedPosition, exception.position());
        assertTrue(exception.getMessage().contains(expectedMessagePart), exception.getMessage());
    }

    @Test
    void namesSimilarToFunctionsAreOrdinaryVariables() {
        assertEquals(5.0, evaluate("sine + expo", Map.of("sine", 2.0, "expo", 3.0)), TOLERANCE);
    }

    @Test
    void implicitMultiplicationWithFunctionsIsRejected() {
        assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse("2sin(x)"));
        assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse("x sin(x)"));
    }

    @Test
    void functionNamesAreRecognisedExactly() {
        assertEquals(MathFunction.LOG10, MathFunction.fromName("log10").orElseThrow());
        assertTrue(MathFunction.fromName("log").isEmpty());
        assertTrue(MathFunction.fromName("SQRT").isEmpty());
    }
}
