package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionParserTest {

    private static final double TOLERANCE = 1e-12;

    private static double evaluate(String input) {
        return ExpressionParser.parse(input).evaluate(Map.of());
    }

    @ParameterizedTest
    @CsvSource({
            "'2+3*4', 14",
            "'(2+3)*4', 20",
            "'2*3+4', 10",
            "'10-4-3', 3",
            "'12/3/2', 2",
            "'8/2*4', 16",
            "'2^3', 8",
            "'2^3^2', 512",
            "'(2^3)^2', 64",
            "'-2^2', -4",
            "'(-2)^2', 4",
            "'2^-1', 0.5",
            "'2^-1^2', 0.5",
            "'-2*-3', 6",
            "'--2', 2",
            "'+2', 2",
            "'-+-2', 2",
            "'3-(-2)', 5",
            "'2*3^2', 18",
            "'-3^2+1', -8",
            "'((((7))))', 7",
            "'1.5e2 / .5', 300",
            "'1 - 2 * (3 + 4) / 7', -1"
    })
    void evaluatesWithDocumentedPrecedenceAndAssociativity(String input, double expected) {
        assertEquals(expected, evaluate(input), TOLERANCE);
    }

    @Test
    void parsesIntoExpectedTreeForPrecedence() {
        Expression expected = new BinaryExpression(BinaryOperator.ADD,
                new NumberExpression(2),
                new BinaryExpression(BinaryOperator.MULTIPLY, new NumberExpression(3), new NumberExpression(4)));

        assertEquals(expected, ExpressionParser.parse("2+3*4"));
    }

    @Test
    void exponentiationIsRightAssociative() {
        Expression expected = new BinaryExpression(BinaryOperator.POWER,
                new NumberExpression(2),
                new BinaryExpression(BinaryOperator.POWER, new NumberExpression(3), new NumberExpression(2)));

        assertEquals(expected, ExpressionParser.parse("2^3^2"));
    }

    @Test
    void subtractionIsLeftAssociative() {
        Expression expected = new BinaryExpression(BinaryOperator.SUBTRACT,
                new BinaryExpression(BinaryOperator.SUBTRACT, new NumberExpression(10), new NumberExpression(4)),
                new NumberExpression(3));

        assertEquals(expected, ExpressionParser.parse("10-4-3"));
    }

    @Test
    void unaryMinusAppliesAfterExponentiation() {
        Expression expected = new NegationExpression(
                new BinaryExpression(BinaryOperator.POWER, new NumberExpression(2), new NumberExpression(2)));

        assertEquals(expected, ExpressionParser.parse("-2^2"));
    }

    @Test
    void divisionIsLeftAssociative() {
        Expression expected = new BinaryExpression(BinaryOperator.DIVIDE,
                new BinaryExpression(BinaryOperator.DIVIDE, new NumberExpression(12), new NumberExpression(3)),
                new NumberExpression(2));

        assertEquals(expected, ExpressionParser.parse("12/3/2"));
    }

    @Test
    void multiplicationAndDivisionShareOneLevel() {
        Expression expected = new BinaryExpression(BinaryOperator.MULTIPLY,
                new BinaryExpression(BinaryOperator.DIVIDE, new NumberExpression(8), new NumberExpression(2)),
                new NumberExpression(4));

        assertEquals(expected, ExpressionParser.parse("8/2*4"));
    }

    @Test
    void additionAndSubtractionShareOneLevel() {
        Expression expected = new BinaryExpression(BinaryOperator.ADD,
                new BinaryExpression(BinaryOperator.SUBTRACT, new NumberExpression(1), new NumberExpression(2)),
                new NumberExpression(3));

        assertEquals(expected, ExpressionParser.parse("1-2+3"));
    }

    @Test
    void exponentiationBindsTighterThanMultiplication() {
        Expression expected = new BinaryExpression(BinaryOperator.MULTIPLY,
                new NumberExpression(2),
                new BinaryExpression(BinaryOperator.POWER, new NumberExpression(3), new NumberExpression(2)));

        assertEquals(expected, ExpressionParser.parse("2*3^2"));
    }

    @Test
    void unaryMinusBindsTighterThanMultiplication() {
        Expression expected = new BinaryExpression(BinaryOperator.MULTIPLY,
                new NegationExpression(new NumberExpression(2)),
                new NumberExpression(3));

        assertEquals(expected, ExpressionParser.parse("-2*3"));
    }

    @Test
    void exponentMayStartWithUnaryMinus() {
        Expression expected = new BinaryExpression(BinaryOperator.POWER,
                new NumberExpression(2),
                new NegationExpression(new NumberExpression(3)));

        assertEquals(expected, ExpressionParser.parse("2^-3"));
    }

    @Test
    void parenthesesOverridePrecedence() {
        Expression expected = new BinaryExpression(BinaryOperator.MULTIPLY,
                new BinaryExpression(BinaryOperator.ADD, new NumberExpression(2), new NumberExpression(3)),
                new NumberExpression(4));

        assertEquals(expected, ExpressionParser.parse("(2+3)*4"));
    }

    @Test
    void unaryPlusLeavesTheOperandUnchanged() {
        assertEquals(new NumberExpression(2), ExpressionParser.parse("+2"));
    }

    @Test
    void evaluatesVariablesFromMap() {
        Expression expression = ExpressionParser.parse("x^3 - x - 2");

        assertEquals(4.0, expression.evaluate(Map.of("x", 2.0)), TOLERANCE);
        assertEquals(-2.0, expression.evaluate(Map.of("x", 0.0)), TOLERANCE);
    }

    @Test
    void sameExpressionCanBeEvaluatedWithSeveralVariables() {
        Expression expression = ExpressionParser.parse("t*y1 - y2/2");

        assertEquals(5.0, expression.evaluate(Map.of("t", 2.0, "y1", 3.0, "y2", 2.0)), TOLERANCE);
    }

    @Test
    void extraVariablesInMapAreIgnored() {
        assertEquals(3.0, ExpressionParser.parse("x+1").evaluate(Map.of("x", 2.0, "y", 9.0)), TOLERANCE);
    }

    @Test
    void missingVariableFailsWithItsName() {
        Expression expression = ExpressionParser.parse("x + y");

        ExpressionEvaluationException exception = assertThrows(ExpressionEvaluationException.class,
                () -> expression.evaluate(Map.of("x", 1.0)));

        assertTrue(exception.getMessage().contains("'y'"), exception.getMessage());
    }

    @Test
    void nonFiniteVariableValueIsRejected() {
        Expression expression = ExpressionParser.parse("x");

        assertThrows(ExpressionEvaluationException.class, () -> expression.evaluate(Map.of("x", Double.NaN)));
        assertThrows(ExpressionEvaluationException.class,
                () -> expression.evaluate(Map.of("x", Double.POSITIVE_INFINITY)));
    }

    @ParameterizedTest
    @CsvSource({"'1/0'", "'1/(2-2)'", "'0/0'", "'x/(x-1)'"})
    void divisionByZeroFails(String input) {
        ExpressionEvaluationException exception = assertThrows(ExpressionEvaluationException.class,
                () -> ExpressionParser.parse(input).evaluate(Map.of("x", 1.0)));

        assertEquals("Division by zero", exception.getMessage());
    }

    @Test
    void divisionByVerySmallNonZeroNumberIsAllowed() {
        assertEquals(1e300, evaluate("1/1e-300"), 1e285);
    }

    @ParameterizedTest
    @CsvSource({"'0^-1'", "'(-8)^(1/3)'", "'1e308*10'", "'10^400'"})
    void nonFiniteResultsFail(String input) {
        assertThrows(ExpressionEvaluationException.class, () -> evaluate(input));
    }

    @Test
    void zeroToThePowerOfZeroIsOne() {
        assertEquals(1.0, evaluate("0^0"), TOLERANCE);
    }

    @ParameterizedTest
    @CsvSource({
            "'', 0",
            "'   ', 3",
            "'2+', 2",
            "'*2', 0",
            "'2*/3', 2",
            "'2 3', 2",
            "'x y', 2",
            "'(2+3', 4",
            "'((2)', 4",
            "'2+3)', 3",
            "')', 0",
            "'()', 1",
            "'2^', 2",
            "'-', 1"
    })
    void rejectsInvalidSyntaxWithPosition(String input, int expectedPosition) {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(input));

        assertEquals(expectedPosition, exception.position());
    }

    @Test
    void emptyInputHasClearMessage() {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(""));

        assertTrue(exception.getMessage().startsWith("Expression is empty"), exception.getMessage());
    }

    @Test
    void missingClosingParenthesisNamesTheOpeningOne() {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse("2*(x+1"));

        assertTrue(exception.getMessage().contains("'(' at column 3"), exception.getMessage());
    }

    @Test
    void unmatchedClosingParenthesisIsReported() {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse("(1+2))"));

        assertTrue(exception.getMessage().startsWith("Unmatched ')'"), exception.getMessage());
        assertEquals(5, exception.position());
    }

    @Test
    void tokenizerErrorsPassThrough() {
        assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse("2x + 1"));
    }

    @Test
    void parsingDoesNotEvaluate() {
        Expression expression = ExpressionParser.parse("1/0");

        assertThrows(ExpressionEvaluationException.class, () -> expression.evaluate(Map.of()));
    }
}
