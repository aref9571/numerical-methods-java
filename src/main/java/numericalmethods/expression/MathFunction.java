package numericalmethods.expression;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

public enum MathFunction {
    SIN("sin", Math::sin),
    COS("cos", Math::cos),
    TAN("tan", Math::tan),
    ASIN("asin", Math::asin),
    ACOS("acos", Math::acos),
    ATAN("atan", Math::atan),
    EXP("exp", Math::exp),
    LN("ln", Math::log),
    LOG10("log10", Math::log10),
    SQRT("sqrt", Math::sqrt),
    ABS("abs", Math::abs);

    private final String functionName;
    private final DoubleUnaryOperator operation;

    MathFunction(String functionName, DoubleUnaryOperator operation) {
        this.functionName = functionName;
        this.operation = operation;
    }

    public String functionName() {
        return functionName;
    }

    public static Optional<MathFunction> fromName(String name) {
        return Arrays.stream(values())
                .filter(function -> function.functionName.equals(name))
                .findFirst();
    }

    public double apply(double argument) {
        double result = operation.applyAsDouble(argument);
        if (!Double.isFinite(result)) {
            throw new ExpressionEvaluationException(
                    functionName + "(" + argument + ") is undefined or not a finite number");
        }
        return result;
    }
}
