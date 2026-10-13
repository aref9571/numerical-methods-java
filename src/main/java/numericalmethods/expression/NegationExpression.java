package numericalmethods.expression;

import java.util.Map;
import java.util.Objects;

public record NegationExpression(Expression operand) implements Expression {

    public NegationExpression {
        Objects.requireNonNull(operand, "operand");
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        return -operand.evaluate(variables);
    }
}
