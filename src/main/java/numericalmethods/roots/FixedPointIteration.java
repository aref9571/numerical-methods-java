package numericalmethods.roots;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import numericalmethods.expression.Differentiator;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionEvaluationException;
import numericalmethods.solver.InputValidation;
import numericalmethods.solver.StoppingCriteria;

public final class FixedPointIteration {

    public static final double DIVERGENCE_LIMIT = 1e100;
    public static final int INTERVAL_SAMPLES = 200;

    private FixedPointIteration() {
    }

    public record Result(
            IterativeRootResult iteration,
            Expression derivative,
            OptionalDouble slopeAtStart,
            OptionalDouble slopeAtEnd,
            OptionalDouble aitkenEstimate,
            OptionalDouble aitkenErrorEstimate) {

        public Result {
            Objects.requireNonNull(iteration, "iteration");
            Objects.requireNonNull(derivative, "derivative");
        }
    }

    public record IntervalCheck(boolean mapsIntoInterval, double lambda) {

        public boolean isContraction() {
            return mapsIntoInterval && lambda < 1;
        }
    }

    public static Result solve(Expression g, String variable, double start, StoppingCriteria criteria) {
        OpenRootFinder.requireArguments(g, variable, criteria);
        InputValidation.requireFinite("Starting value", start);
        Expression derivative = Differentiator.differentiate(g, variable);
        IterativeRootResult iteration = iterate(g, variable, start, criteria);
        List<Double> iterates = iteration.iterates();
        return new Result(iteration, derivative,
                slope(derivative, variable, start),
                slope(derivative, variable, iteration.result().root()),
                aitken(iterates, false),
                aitken(iterates, true));
    }

    public static IntervalCheck checkInterval(Expression g, String variable, double lower, double upper) {
        InputValidation.requireFinite("Lower endpoint", lower);
        InputValidation.requireFinite("Upper endpoint", upper);
        if (lower >= upper) {
            throw new IllegalArgumentException("The lower endpoint must be less than the upper endpoint");
        }
        Expression derivative = Differentiator.differentiate(g, variable);
        boolean mapsInto = true;
        double lambda = 0;
        for (int i = 0; i <= INTERVAL_SAMPLES; i++) {
            double x = lower + (upper - lower) * i / INTERVAL_SAMPLES;
            try {
                double gx = OpenRootFinder.evaluate(g, variable, x);
                if (gx < lower || gx > upper) {
                    mapsInto = false;
                }
                lambda = Math.max(lambda, Math.abs(OpenRootFinder.evaluate(derivative, variable, x)));
            } catch (ExpressionEvaluationException undefined) {
                return new IntervalCheck(false, Double.POSITIVE_INFINITY);
            }
        }
        return new IntervalCheck(mapsInto, lambda);
    }

    public static double contractionBound(double lambda, int n, double firstStep) {
        if (lambda >= 1 || lambda < 0) {
            throw new IllegalArgumentException("The contraction bound needs 0 <= lambda < 1");
        }
        return Math.pow(lambda, n) / (1 - lambda) * Math.abs(firstStep);
    }

    private static IterativeRootResult iterate(Expression g, String variable, double start, StoppingCriteria criteria) {
        List<Double> iterates = new ArrayList<>(List.of(start));
        double x = start;
        double gx = OpenRootFinder.startValue(g, variable, start);
        for (int iteration = 1; iteration <= criteria.maxIterations(); iteration++) {
            double next = gx;
            if (!Double.isFinite(next) || Math.abs(next) > DIVERGENCE_LIMIT) {
                return new IterativeRootResult(RootResult.breakdown(x, gx - x, 0.0, iteration - 1,
                        "The iteration diverged (|x| grew beyond " + DIVERGENCE_LIMIT + ")"), iterates);
            }
            iterates.add(next);
            double gNext;
            try {
                gNext = OpenRootFinder.evaluate(g, variable, next);
            } catch (ExpressionEvaluationException undefined) {
                return new IterativeRootResult(RootResult.breakdown(x, gx - x, Math.abs(next - x), iteration - 1,
                        "g is undefined at " + variable + " = " + next + " (" + undefined.getMessage() + ")"),
                        iterates);
            }
            double change = Math.abs(next - x);
            x = next;
            gx = gNext;
            if (!Double.isFinite(gx - x)) {
                return new IterativeRootResult(RootResult.breakdown(x, 0.0, change, iteration,
                        "The iteration diverged (g(x) is too large)"), iterates);
            }
            if (criteria.isWithinTolerance(change, x)) {
                return new IterativeRootResult(RootResult.converged(x, gx - x, change, iteration), iterates);
            }
        }
        double lastChange = Math.abs(iterates.get(iterates.size() - 1) - iterates.get(iterates.size() - 2));
        return new IterativeRootResult(RootResult.iterationLimitReached(x, gx - x, lastChange,
                criteria.maxIterations()), iterates);
    }

    private static OptionalDouble slope(Expression derivative, String variable, double x) {
        try {
            return OptionalDouble.of(OpenRootFinder.evaluate(derivative, variable, x));
        } catch (ExpressionEvaluationException undefined) {
            return OptionalDouble.empty();
        }
    }

    private static OptionalDouble aitken(List<Double> iterates, boolean errorOnly) {
        int n = iterates.size() - 1;
        if (n < 2) {
            return OptionalDouble.empty();
        }
        double latestStep = iterates.get(n) - iterates.get(n - 1);
        double previousStep = iterates.get(n - 1) - iterates.get(n - 2);
        if (previousStep == 0 || latestStep == 0) {
            return OptionalDouble.empty();
        }
        double lambda = latestStep / previousStep;
        if (lambda == 1 || !Double.isFinite(lambda)) {
            return OptionalDouble.empty();
        }
        double correction = lambda / (1 - lambda) * latestStep;
        if (!Double.isFinite(correction)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(errorOnly ? correction : iterates.get(n) + correction);
    }
}
