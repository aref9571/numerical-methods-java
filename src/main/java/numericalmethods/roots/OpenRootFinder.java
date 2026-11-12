package numericalmethods.roots;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import numericalmethods.expression.Differentiator;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionEvaluationException;
import numericalmethods.solver.InputValidation;
import numericalmethods.solver.StoppingCriteria;

public final class OpenRootFinder {

    private OpenRootFinder() {
    }

    public static IterativeRootResult newton(
            Expression function, String variable, double start, StoppingCriteria criteria) {
        requireArguments(function, variable, criteria);
        InputValidation.requireFinite("Starting value", start);
        Expression derivative = Differentiator.differentiate(function, variable);
        return newton(function, derivative, variable, start, criteria);
    }

    public static IterativeRootResult newton(
            Expression function, Expression derivative, String variable, double start, StoppingCriteria criteria) {
        requireArguments(function, variable, criteria);
        Objects.requireNonNull(derivative, "derivative");
        InputValidation.requireFinite("Starting value", start);
        List<Double> iterates = new ArrayList<>(List.of(start));
        double x = start;
        double fx = startValue(function, variable, start);
        if (fx == 0) {
            return done(RootResult.converged(x, 0.0, 0.0, 0), iterates);
        }
        for (int iteration = 1; iteration <= criteria.maxIterations(); iteration++) {
            double slope;
            try {
                slope = evaluate(derivative, variable, x);
            } catch (ExpressionEvaluationException undefined) {
                return done(RootResult.breakdown(x, fx, 0.0, iteration - 1, "The derivative is undefined at "
                        + variable + " = " + x + " (" + undefined.getMessage() + ")"), iterates);
            }
            double step = fx / slope;
            if (slope == 0 || !Double.isFinite(step)) {
                return done(RootResult.breakdown(x, fx, 0.0, iteration - 1, "The derivative is zero or too small at "
                        + variable + " = " + x + " (f'(x) = " + slope + "), so the Newton step is undefined;"
                        + " try another starting value"), iterates);
            }
            double next = x - step;
            if (!Double.isFinite(next)) {
                return done(RootResult.breakdown(x, fx, Math.abs(step), iteration - 1,
                        "The next Newton iterate is not a finite number"), iterates);
            }
            double fNext;
            try {
                fNext = evaluate(function, variable, next);
            } catch (ExpressionEvaluationException undefined) {
                return done(RootResult.breakdown(x, fx, Math.abs(step), iteration - 1, "The function is undefined at "
                        + variable + " = " + next + " (" + undefined.getMessage() + ")"), iterates);
            }
            iterates.add(next);
            double change = Math.abs(next - x);
            x = next;
            fx = fNext;
            if (fx == 0 || criteria.isWithinTolerance(change, x)) {
                return done(RootResult.converged(x, fx, change, iteration), iterates);
            }
        }
        return done(RootResult.iterationLimitReached(x, fx, Math.abs(iterates.get(iterates.size() - 1)
                - iterates.get(iterates.size() - 2)), criteria.maxIterations()), iterates);
    }

    public static IterativeRootResult secant(
            Expression function, String variable, double first, double second, StoppingCriteria criteria) {
        requireArguments(function, variable, criteria);
        InputValidation.requireFinite("First starting value", first);
        InputValidation.requireFinite("Second starting value", second);
        if (first == second) {
            throw new IllegalArgumentException("The secant method needs two different starting values");
        }
        List<Double> iterates = new ArrayList<>(List.of(first, second));
        double previous = first;
        double fPrevious = startValue(function, variable, first);
        double x = second;
        double fx = startValue(function, variable, second);
        if (fx == 0) {
            return done(RootResult.converged(x, 0.0, 0.0, 0), iterates);
        }
        boolean previousStepSmall = false;
        for (int iteration = 1; iteration <= criteria.maxIterations(); iteration++) {
            double denominator = fx - fPrevious;
            double next = x - fx * (x - previous) / denominator;
            if (denominator == 0 || !Double.isFinite(next)) {
                return done(RootResult.breakdown(x, fx, Math.abs(x - previous), iteration - 1,
                        "f has the same value at the last two points (" + previous + " and " + x
                                + "), so the secant line is flat"), iterates);
            }
            double fNext;
            try {
                fNext = evaluate(function, variable, next);
            } catch (ExpressionEvaluationException undefined) {
                return done(RootResult.breakdown(x, fx, Math.abs(x - previous), iteration - 1,
                        "The function is undefined at " + variable + " = " + next + " ("
                                + undefined.getMessage() + ")"), iterates);
            }
            iterates.add(next);
            double change = Math.abs(next - x);
            boolean small = criteria.isWithinTolerance(change, next);
            previous = x;
            fPrevious = fx;
            x = next;
            fx = fNext;
            if (fx == 0 || small && previousStepSmall) {
                return done(RootResult.converged(x, fx, change, iteration), iterates);
            }
            if (change == 0) {
                return done(RootResult.breakdown(x, fx, Math.abs(x - previous), iteration,
                        "The iteration stalled at " + variable + " = " + x + " where f = " + fx
                                + " is not zero: the secant line was almost vertical, so this is not a root"),
                        iterates);
            }
            previousStepSmall = small;
        }
        return done(RootResult.iterationLimitReached(x, fx, Math.abs(x - previous), criteria.maxIterations()),
                iterates);
    }

    static void requireArguments(Expression function, String variable, StoppingCriteria criteria) {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(variable, "variable");
        Objects.requireNonNull(criteria, "criteria");
    }

    static double startValue(Expression function, String variable, double x) {
        try {
            return evaluate(function, variable, x);
        } catch (ExpressionEvaluationException undefined) {
            throw new IllegalArgumentException("The function is undefined at the starting value " + variable + " = "
                    + x + " (" + undefined.getMessage() + ")", undefined);
        }
    }

    static double evaluate(Expression function, String variable, double x) {
        return function.evaluate(Map.of(variable, x));
    }

    private static IterativeRootResult done(RootResult result, List<Double> iterates) {
        return new IterativeRootResult(result, iterates);
    }
}
