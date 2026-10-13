package numericalmethods.expression;

import java.util.Map;
import java.util.Objects;

public record FunctionExpression(MathFunction function, Expression argument) implements Expression {

    public FunctionExpression {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(argument, "argument");
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        return function.apply(argument.evaluate(variables));
    }
}
