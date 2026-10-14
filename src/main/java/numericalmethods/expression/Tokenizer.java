package numericalmethods.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Tokenizer {

    public static final int MAX_INPUT_LENGTH = 1000;

    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private int index;

    private Tokenizer(String input) {
        this.input = input;
    }

    public static List<Token> tokenize(String input) {
        Objects.requireNonNull(input, "input");
        if (input.length() > MAX_INPUT_LENGTH) {
            throw new ExpressionSyntaxException("Expression is too long: " + input.length()
                    + " characters, the limit is " + MAX_INPUT_LENGTH, MAX_INPUT_LENGTH);
        }
        return new Tokenizer(input).readAllTokens();
    }

    private List<Token> readAllTokens() {
        while (index < input.length()) {
            char current = input.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (isDigit(current) || current == '.') {
                readNumber();
            } else if (isLetter(current)) {
                readIdentifier();
            } else {
                readSymbol(current);
            }
        }
        tokens.add(new Token(TokenType.END, "", input.length()));
        return List.copyOf(tokens);
    }

    private void readNumber() {
        int start = index;
        skipDigits();
        if (currentCharIs('.')) {
            index++;
            skipDigits();
        }
        if (index == start + 1 && input.charAt(start) == '.') {
            throw new ExpressionSyntaxException("Malformed number '" + input.substring(start, index) + "'", start);
        }
        if (startsExponent()) {
            readExponent(start);
        }
        rejectInvalidCharacterAfterNumber(start);

        String text = input.substring(start, index);
        if (Double.isInfinite(Double.parseDouble(text))) {
            throw new ExpressionSyntaxException("Number '" + text + "' is too large", start);
        }
        tokens.add(new Token(TokenType.NUMBER, text, start));
    }

    private boolean startsExponent() {
        if (!currentCharIs('e') && !currentCharIs('E')) {
            return false;
        }
        int next = index + 1;
        if (next >= input.length()) {
            return false;
        }
        char afterE = input.charAt(next);
        return isDigit(afterE) || afterE == '+' || afterE == '-';
    }

    private void readExponent(int numberStart) {
        index++;
        if (currentCharIs('+') || currentCharIs('-')) {
            index++;
        }
        int exponentDigitsStart = index;
        skipDigits();
        if (index == exponentDigitsStart) {
            throw new ExpressionSyntaxException(
                    "Malformed number '" + input.substring(numberStart, index) + "': exponent has no digits",
                    numberStart);
        }
    }

    private void rejectInvalidCharacterAfterNumber(int numberStart) {
        if (index >= input.length()) {
            return;
        }
        char next = input.charAt(index);
        if (next == '.') {
            throw new ExpressionSyntaxException(
                    "Malformed number '" + input.substring(numberStart, index + 1) + "'", numberStart);
        }
        if (isLetter(next)) {
            throw new ExpressionSyntaxException(
                    "Missing operator between '" + input.substring(numberStart, index) + "' and '" + next
                            + "'; multiplication must be written explicitly, for example 2*x",
                    index);
        }
    }

    private void readIdentifier() {
        int start = index;
        while (index < input.length() && (isLetter(input.charAt(index)) || isDigit(input.charAt(index)))) {
            index++;
        }
        tokens.add(new Token(TokenType.IDENTIFIER, input.substring(start, index), start));
    }

    private void readSymbol(char symbol) {
        TokenType type = switch (symbol) {
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.STAR;
            case '/' -> TokenType.SLASH;
            case '^' -> TokenType.CARET;
            case '(' -> TokenType.LEFT_PAREN;
            case ')' -> TokenType.RIGHT_PAREN;
            case ',' -> TokenType.COMMA;
            default -> throw new ExpressionSyntaxException("Unexpected character '" + symbol + "'", index);
        };
        tokens.add(new Token(type, String.valueOf(symbol), index));
        index++;
    }

    private void skipDigits() {
        while (index < input.length() && isDigit(input.charAt(index))) {
            index++;
        }
    }

    private boolean currentCharIs(char expected) {
        return index < input.length() && input.charAt(index) == expected;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }
}
