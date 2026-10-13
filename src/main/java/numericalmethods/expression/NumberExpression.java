package numericalmethods.expression;

import java.util.Map;

public record NumberExpression(double value) implements Expression {

    @Override
    public double evaluate(Map<String, Double> variables) {
        return value;
    }
}
