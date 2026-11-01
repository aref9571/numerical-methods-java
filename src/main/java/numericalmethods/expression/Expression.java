package numericalmethods.expression;

import java.util.Map;

public sealed interface Expression permits NumberExpression, ConstantExpression, VariableExpression,
        NegationExpression, BinaryExpression, FunctionExpression {

    double evaluate(Map<String, Double> variables);
}
