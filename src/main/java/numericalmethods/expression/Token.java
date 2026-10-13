package numericalmethods.expression;

import java.util.Objects;

public record Token(TokenType type, String text, int position) {

    public Token {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
        if (position < 0) {
            throw new IllegalArgumentException("Position must not be negative: " + position);
        }
    }

    public double numberValue() {
        if (type != TokenType.NUMBER) {
            throw new IllegalStateException("Token " + type + " is not a number");
        }
        return Double.parseDouble(text);
    }
}
