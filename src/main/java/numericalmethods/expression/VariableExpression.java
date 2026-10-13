package numericalmethods.expression;

import java.util.Map;
import java.util.Objects;

public record VariableExpression(String name) implements Expression {

    public VariableExpression {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        Double value = variables.get(name);
        if (value == null) {
            throw new ExpressionEvaluationException("No value given for variable '" + name + "'");
        }
        if (!Double.isFinite(value)) {
            throw new ExpressionEvaluationException("Variable '" + name + "' must be a finite number, but was " + value);
        }
        return value;
    }
}
