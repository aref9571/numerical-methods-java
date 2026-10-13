package numericalmethods.expression;

import java.util.Map;
import java.util.Objects;

public record BinaryExpression(BinaryOperator operator, Expression left, Expression right) implements Expression {

    public BinaryExpression {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        return operator.apply(left.evaluate(variables), right.evaluate(variables));
    }
}
