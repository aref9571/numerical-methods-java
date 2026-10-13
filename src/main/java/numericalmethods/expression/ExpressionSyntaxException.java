package numericalmethods.expression;

public class ExpressionSyntaxException extends RuntimeException {

    private final int position;

    public ExpressionSyntaxException(String message, int position) {
        super(message + " at column " + (position + 1));
        this.position = position;
    }

    public int position() {
        return position;
    }
}
