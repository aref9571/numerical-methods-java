package numericalmethods.expression;

import java.util.Map;

public sealed interface Expression permits NumberExpression, VariableExpression, NegationExpression, BinaryExpression,
        FunctionExpression {

    double evaluate(Map<String, Double> variables);
}
