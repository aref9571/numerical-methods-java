package numericalmethods.expression;

import java.util.List;

public final class ExpressionParser {

    private final List<Token> tokens;
    private int current;

    private ExpressionParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Expression parse(String input) {
        return new ExpressionParser(Tokenizer.tokenize(input)).parseCompleteExpression();
    }

    private Expression parseCompleteExpression() {
        if (peek().type() == TokenType.END) {
            throw new ExpressionSyntaxException("Expression is empty", peek().position());
        }
        Expression expression = parseSum();
        Token next = peek();
        if (next.type() == TokenType.RIGHT_PAREN) {
            throw new ExpressionSyntaxException("Unmatched ')'", next.position());
        }
        if (next.type() != TokenType.END) {
            throw new ExpressionSyntaxException(
                    "Unexpected '" + next.text() + "' after a complete expression", next.position());
        }
        return expression;
    }

    private Expression parseSum() {
        Expression expression = parseProduct();
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            BinaryOperator operator = advance().type() == TokenType.PLUS
                    ? BinaryOperator.ADD
                    : BinaryOperator.SUBTRACT;
            expression = new BinaryExpression(operator, expression, parseProduct());
        }
        return expression;
    }

    private Expression parseProduct() {
        Expression expression = parseSignedFactor();
        while (peek().type() == TokenType.STAR || peek().type() == TokenType.SLASH) {
            BinaryOperator operator = advance().type() == TokenType.STAR
                    ? BinaryOperator.MULTIPLY
                    : BinaryOperator.DIVIDE;
            expression = new BinaryExpression(operator, expression, parseSignedFactor());
        }
        return expression;
    }

    private Expression parseSignedFactor() {
        if (peek().type() == TokenType.MINUS) {
            advance();
            return new NegationExpression(parseSignedFactor());
        }
        if (peek().type() == TokenType.PLUS) {
            advance();
            return parseSignedFactor();
        }
        return parsePower();
    }

    private Expression parsePower() {
        Expression base = parsePrimary();
        if (peek().type() == TokenType.CARET) {
            advance();
            return new BinaryExpression(BinaryOperator.POWER, base, parseSignedFactor());
        }
        return base;
    }

    private Expression parsePrimary() {
        Token token = advance();
        return switch (token.type()) {
            case NUMBER -> new NumberExpression(token.numberValue());
            case IDENTIFIER -> new VariableExpression(token.text());
            case LEFT_PAREN -> parseParenthesized(token);
            case END -> throw new ExpressionSyntaxException("Expression ends where a value was expected", token.position());
            default -> throw new ExpressionSyntaxException(
                    "Expected a number, variable or '(' but found '" + token.text() + "'", token.position());
        };
    }

    private Expression parseParenthesized(Token openParenthesis) {
        Expression inner = parseSum();
        if (peek().type() != TokenType.RIGHT_PAREN) {
            throw new ExpressionSyntaxException("Missing ')' for the '(' at column " + (openParenthesis.position() + 1),
                    peek().position());
        }
        advance();
        return inner;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        Token token = tokens.get(current);
        if (token.type() != TokenType.END) {
            current++;
        }
        return token;
    }
}
