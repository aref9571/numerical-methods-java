package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TokenizerTest {

    @Test
    void emptyInputProducesOnlyEndToken() {
        List<Token> tokens = Tokenizer.tokenize("");

        assertEquals(List.of(new Token(TokenType.END, "", 0)), tokens);
    }

    @Test
    void whitespaceOnlyInputProducesOnlyEndToken() {
        List<Token> tokens = Tokenizer.tokenize(" \t\n ");

        assertEquals(List.of(new Token(TokenType.END, "", 4)), tokens);
    }

    @Test
    void tokenizesAllOperatorsParenthesesAndComma() {
        List<Token> tokens = Tokenizer.tokenize("+-*/^(),");

        assertEquals(List.of(
                TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
                TokenType.CARET, TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN, TokenType.COMMA, TokenType.END),
                types(tokens));
    }

    @Test
    void tokenizesExpressionWithPositions() {
        List<Token> tokens = Tokenizer.tokenize("x^3 - 2*x");

        assertEquals(List.of(
                new Token(TokenType.IDENTIFIER, "x", 0),
                new Token(TokenType.CARET, "^", 1),
                new Token(TokenType.NUMBER, "3", 2),
                new Token(TokenType.MINUS, "-", 4),
                new Token(TokenType.NUMBER, "2", 6),
                new Token(TokenType.STAR, "*", 7),
                new Token(TokenType.IDENTIFIER, "x", 8),
                new Token(TokenType.END, "", 9)), tokens);
    }

    @Test
    void whitespaceDoesNotChangeTokenTypes() {
        assertEquals(types(Tokenizer.tokenize("sin(x)+1")), types(Tokenizer.tokenize("  sin ( x )\t+ 1 ")));
    }

    @Test
    void tokenizesFunctionCallWithMultiCharacterNames() {
        List<Token> tokens = Tokenizer.tokenize("log10(y1)");

        assertEquals(List.of(
                new Token(TokenType.IDENTIFIER, "log10", 0),
                new Token(TokenType.LEFT_PAREN, "(", 5),
                new Token(TokenType.IDENTIFIER, "y1", 6),
                new Token(TokenType.RIGHT_PAREN, ")", 8),
                new Token(TokenType.END, "", 9)), tokens);
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "t", "y", "y1", "y2", "pi", "e", "sqrt", "atan", "xValue2"})
    void tokenizesIdentifiers(String identifier) {
        List<Token> tokens = Tokenizer.tokenize(identifier);

        assertEquals(new Token(TokenType.IDENTIFIER, identifier, 0), tokens.get(0));
        assertEquals(TokenType.END, tokens.get(1).type());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0.0",
            "42, 42.0",
            "2.5, 2.5",
            ".25, 0.25",
            "5., 5.0",
            "1e-6, 0.000001",
            "1E6, 1000000.0",
            "2.5e+3, 2500.0",
            "007, 7.0",
            "0e-400, 0.0"
    })
    void tokenizesNumbers(String text, double expectedValue) {
        List<Token> tokens = Tokenizer.tokenize(text);

        assertEquals(2, tokens.size());
        assertEquals(new Token(TokenType.NUMBER, text, 0), tokens.get(0));
        assertEquals(expectedValue, tokens.get(0).numberValue(), 1e-15);
    }

    @Test
    void numberFollowedByOperatorIsSplitCorrectly() {
        List<Token> tokens = Tokenizer.tokenize("1e-6-x");

        assertEquals(List.of(TokenType.NUMBER, TokenType.MINUS, TokenType.IDENTIFIER, TokenType.END), types(tokens));
        assertEquals("1e-6", tokens.get(0).text());
    }

    @ParameterizedTest
    @CsvSource({
            "'.', 0",
            "'1.2.3', 0",
            "'2..5', 0",
            "'1e', 1",
            "'1e+', 0",
            "'1e-x', 0",
            "'3 + 1.5.2', 4",
            "'2x', 1",
            "'3sin(x)', 1",
            "'1e999', 0",
            "'1e-400', 0",
            "'2 + 0.5e-320', 4",
            "'1e-308', 0"
    })
    void rejectsMalformedNumbers(String input, int expectedPosition) {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> Tokenizer.tokenize(input));

        assertEquals(expectedPosition, exception.position());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", ".0", "0.000e5", "2.3e-308", "1e-300"})
    void acceptsZeroAndSmallNormalNumbers(String input) {
        assertEquals(TokenType.NUMBER, Tokenizer.tokenize(input).get(0).type());
    }

    @Test
    void tooSmallNumberHasClearMessage() {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> Tokenizer.tokenize("1e-400"));

        assertTrue(exception.getMessage().startsWith("Number '1e-400' is too small"), exception.getMessage());
    }

    @Test
    void implicitMultiplicationMessageExplainsExplicitMultiplication() {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> Tokenizer.tokenize("2x"));

        assertTrue(exception.getMessage().contains("2*x"), exception.getMessage());
        assertTrue(exception.getMessage().contains("column 2"), exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "'x # 2', 2",
            "'2 % 3', 2",
            "'y_1', 1",
            "'x = 1', 2",
            "'2 − 1', 2",
            "'١', 0",
            "'é', 0"
    })
    void rejectsUnexpectedCharacters(String input, int expectedPosition) {
        ExpressionSyntaxException exception =
                assertThrows(ExpressionSyntaxException.class, () -> Tokenizer.tokenize(input));

        assertEquals(expectedPosition, exception.position());
        assertTrue(exception.getMessage().startsWith("Unexpected character"), exception.getMessage());
    }

    @Test
    void rejectsNullInput() {
        assertThrows(NullPointerException.class, () -> Tokenizer.tokenize(null));
    }

    @Test
    void tokenizationIsDeterministic() {
        String input = "sqrt(x^2 + y1^2) / 1.5e-3";

        assertEquals(Tokenizer.tokenize(input), Tokenizer.tokenize(input));
    }

    @Test
    void returnedListCannotBeModified() {
        List<Token> tokens = Tokenizer.tokenize("x");

        assertThrows(UnsupportedOperationException.class, () -> tokens.add(tokens.get(0)));
    }

    @Test
    void numberValueRejectsNonNumberToken() {
        Token token = new Token(TokenType.IDENTIFIER, "x", 0);

        assertThrows(IllegalStateException.class, token::numberValue);
    }

    private static List<TokenType> types(List<Token> tokens) {
        return tokens.stream().map(Token::type).toList();
    }
}
