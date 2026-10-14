package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionLimitsTest {

    private static final double TOLERANCE = 1e-12;

    private static String nestedInParentheses(String inner, int levels) {
        return "(".repeat(levels) + inner + ")".repeat(levels);
    }

    @Test
    void inputAtTheLengthLimitIsAccepted() {
        String input = "1" + " ".repeat(Tokenizer.MAX_INPUT_LENGTH - 1);

        assertEquals(1.0, ExpressionParser.parse(input).evaluate(Map.of()), TOLERANCE);
    }

    @Test
    void inputOverTheLengthLimitIsRejected() {
        String input = "1" + " ".repeat(Tokenizer.MAX_INPUT_LENGTH);

        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(input));

        assertTrue(exception.getMessage().startsWith("Expression is too long: 1001 characters"),
                exception.getMessage());
        assertEquals(Tokenizer.MAX_INPUT_LENGTH, exception.position());
    }

    @Test
    void veryLongInputIsRejectedBeforeTokenizing() {
        String input = nestedInParentheses("1", 1_000_000);

        assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(input));
    }

    @Test
    void moderateNestingIsAccepted() {
        assertEquals(7.0, ExpressionParser.parse(nestedInParentheses("7", 50)).evaluate(Map.of()), TOLERANCE);
    }

    @Test
    void longFlatExpressionWithinLimitsEvaluates() {
        String input = "1+".repeat(499) + "1";

        assertEquals(500.0, ExpressionParser.parse(input).evaluate(Map.of()), TOLERANCE);
    }

    static Stream<String> deeplyNestedInputs() {
        return Stream.of(
                nestedInParentheses("1", 150),
                "(".repeat(999),
                "-".repeat(500) + "1",
                "2^".repeat(300) + "2",
                "sin(".repeat(150) + "x" + ")".repeat(150),
                "(-".repeat(300) + "1" + ")".repeat(300));
    }

    @ParameterizedTest
    @MethodSource("deeplyNestedInputs")
    void deeplyNestedInputIsRejectedWithoutStackOverflow(String input) {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(input));

        assertTrue(exception.getMessage().startsWith("Expression is nested too deeply"), exception.getMessage());
    }

    @Test
    void parserCanBeReusedAfterRejectingDeepInput() {
        assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(nestedInParentheses("1", 150)));

        assertEquals(3.0, ExpressionParser.parse("1+2").evaluate(Map.of()), TOLERANCE);
    }

    static Stream<String> malformedInputs() {
        return Stream.of(
                "\u0000",
                "1\t+\u00002",
                "x²",
                "٣+1",
                "1,2",
                "sin(1,2)",
                "((((",
                "))))",
                "1++",
                "^2",
                "2^^2",
                "e(1)",
                "pi()",
                "sin",
                ".",
                "1..2",
                "1e",
                "1e+",
                "--",
                "()()");
    }

    @ParameterizedTest
    @MethodSource("malformedInputs")
    void malformedInputFailsWithSyntaxErrorOnly(String input) {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse(input));

        assertTrue(exception.position() >= 0 && exception.position() <= input.length(),
                "position " + exception.position() + " for input of length " + input.length());
        assertTrue(exception.getMessage().contains(" at column "), exception.getMessage());
    }
}
