package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressionFormatterTest {

    private static String formatInput(String input) {
        return ExpressionFormatter.format(ExpressionParser.parse(input));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "-x^2 | -(x^2)",
            "x^3 - x - 2 | ((x^3) - x) - 2",
            "2+3*4 | 2 + (3 * 4)",
            "(2+3)*4 | (2 + 3) * 4",
            "2^3^2 | 2^(3^2)",
            "2^-1 | 2^(-1)",
            "--x | -(-x)",
            "+x | x",
            "-2*3 | (-2) * 3",
            "sin(pi/6) | sin(pi / 6)",
            "exp(-t)*y1 | exp(-t) * y1",
            "sqrt( x ^ 2 + 1 ) | sqrt((x^2) + 1)",
            "e | e"
    })
    void formatsFullyBracketed(String input, String expected) {
        assertEquals(expected, formatInput(input));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "007 | 7",
            "2.50 | 2.5",
            ".25 | 0.25",
            "1e-6 | 1.0E-6",
            "2.5E+3 | 2500",
            "1e20 | 1.0E20",
            "0.1 | 0.1"
    })
    void formatsNumbersCompactly(String input, String expected) {
        assertEquals(expected, formatInput(input));
    }

    @Test
    void negativeNumbersAreBracketedAsOperands() {
        Expression expression = new BinaryExpression(BinaryOperator.POWER,
                new VariableExpression("x"), new NumberExpression(-0.5));

        assertEquals("x^(-0.5)", ExpressionFormatter.format(expression));
        assertEquals("-3", ExpressionFormatter.format(new NumberExpression(-3)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "x^3 - x - 2",
            "-x^2 + 2^-3^2",
            "sin(x)^2 + cos(x)^2",
            "exp(-x^2/2) / sqrt(2*pi)",
            "1e-6 * x - 2.5E+3 / (y1 - y2)",
            "--+-x",
            "log10(abs(x - 3)) * atan(x) ^ e"
    })
    void formattedTextParsesBackToTheSameExpression(String input) {
        Expression original = ExpressionParser.parse(input);

        assertEquals(original, ExpressionParser.parse(ExpressionFormatter.format(original)));
    }
}
