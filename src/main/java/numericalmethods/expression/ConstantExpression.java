package numericalmethods.expression;

import java.util.Map;
import java.util.Objects;

public record ConstantExpression(String name, double value) implements Expression {

    public ConstantExpression {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        return value;
    }
}
