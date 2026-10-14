package numericalmethods.expression;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExpressionParser {

    public static final int MAX_NESTING_DEPTH = 100;

    private static final Map<String, Double> CONSTANTS = Map.of("pi", Math.PI, "e", Math.E);

    private final List<Token> tokens;
    private int current;
    private int depth;

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
        depth++;
        if (depth > MAX_NESTING_DEPTH) {
            throw new ExpressionSyntaxException(
                    "Expression is nested too deeply, the limit is " + MAX_NESTING_DEPTH + " levels", peek().position());
        }
        Expression factor = parseSignOrPower();
        depth--;
        return factor;
    }

    private Expression parseSignOrPower() {
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
            case IDENTIFIER -> parseIdentifier(token);
            case LEFT_PAREN -> parseParenthesized(token);
            case END -> throw new ExpressionSyntaxException("Expression ends where a value was expected", token.position());
            default -> throw new ExpressionSyntaxException(
                    "Expected a number, variable or '(' but found '" + token.text() + "'", token.position());
        };
    }

    private Expression parseIdentifier(Token identifier) {
        String name = identifier.text();
        if (peek().type() == TokenType.LEFT_PAREN) {
            return parseFunctionCall(identifier);
        }
        if (CONSTANTS.containsKey(name)) {
            return new NumberExpression(CONSTANTS.get(name));
        }
        if (MathFunction.fromName(name).isPresent()) {
            throw new ExpressionSyntaxException(
                    "Function '" + name + "' must be followed by '(', for example " + name + "(x)",
                    identifier.position());
        }
        return new VariableExpression(name);
    }

    private Expression parseFunctionCall(Token identifier) {
        String name = identifier.text();
        Optional<MathFunction> function = MathFunction.fromName(name);
        if (function.isEmpty()) {
            throw new ExpressionSyntaxException("Unknown function '" + name + "'", identifier.position());
        }
        Token openParenthesis = advance();
        if (peek().type() == TokenType.RIGHT_PAREN) {
            throw new ExpressionSyntaxException("Function '" + name + "' needs one argument", peek().position());
        }
        Expression argument = parseSum();
        if (peek().type() == TokenType.COMMA) {
            throw new ExpressionSyntaxException(
                    "Function '" + name + "' takes exactly one argument", peek().position());
        }
        expectClosingParenthesis(openParenthesis);
        return new FunctionExpression(function.get(), argument);
    }

    private Expression parseParenthesized(Token openParenthesis) {
        Expression inner = parseSum();
        expectClosingParenthesis(openParenthesis);
        return inner;
    }

    private void expectClosingParenthesis(Token openParenthesis) {
        if (peek().type() != TokenType.RIGHT_PAREN) {
            throw new ExpressionSyntaxException("The '(' at column " + (openParenthesis.position() + 1)
                    + " is never closed, expected ')'", peek().position());
        }
        advance();
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
