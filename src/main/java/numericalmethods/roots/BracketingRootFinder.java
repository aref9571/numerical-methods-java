package numericalmethods.roots;

import java.util.Map;
import java.util.Objects;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionEvaluationException;
import numericalmethods.solver.InputValidation;
import numericalmethods.solver.StoppingCriteria;

public final class BracketingRootFinder {

    private BracketingRootFinder() {
    }

    public static RootResult bisection(
            Expression function, String variable, double lower, double upper, StoppingCriteria criteria) {
        Bracket start = Bracket.validated(function, variable, lower, upper, criteria);
        if (start.hasEndpointRoot()) {
            return start.endpointRoot();
        }

        double a = start.lower();
        double fa = start.fLower();
        double b = start.upper();
        double approximation = start.betterEndpoint();
        double fApproximation = start.fBetterEndpoint();
        for (int iteration = 1; iteration <= criteria.maxIterations(); iteration++) {
            double midpoint = a + (b - a) / 2;
            if (midpoint <= a || midpoint >= b) {
                return RootResult.breakdown(approximation, fApproximation, b - a, iteration - 1,
                        "The interval [" + a + ", " + b + "] cannot be split further in double precision;"
                                + " the tolerance is too small for a root near " + variable + " = " + midpoint);
            }
            double fMidpoint;
            try {
                fMidpoint = evaluate(function, variable, midpoint);
            } catch (ExpressionEvaluationException undefined) {
                return undefinedInside(variable, midpoint, undefined, approximation, fApproximation, b - a,
                        iteration - 1);
            }
            approximation = midpoint;
            fApproximation = fMidpoint;
            if (fMidpoint == 0) {
                return RootResult.converged(midpoint, 0.0, 0.0, iteration);
            }
            if (Math.signum(fMidpoint) == Math.signum(fa)) {
                a = midpoint;
                fa = fMidpoint;
            } else {
                b = midpoint;
            }
            if (criteria.isWithinTolerance(b - a, midpoint)) {
                return finish(start, variable, midpoint, fMidpoint, b - a, iteration);
            }
        }
        return RootResult.iterationLimitReached(approximation, fApproximation, b - a, criteria.maxIterations());
    }

    public static RootResult falsePosition(
            Expression function, String variable, double lower, double upper, StoppingCriteria criteria) {
        Bracket start = Bracket.validated(function, variable, lower, upper, criteria);
        if (start.hasEndpointRoot()) {
            return start.endpointRoot();
        }

        double a = start.lower();
        double fa = start.fLower();
        double b = start.upper();
        double fb = start.fUpper();
        double approximation = start.betterEndpoint();
        double fApproximation = start.fBetterEndpoint();
        for (int iteration = 1; iteration <= criteria.maxIterations(); iteration++) {
            double point = falsePositionPoint(a, fa, b, fb);
            double fPoint;
            try {
                fPoint = evaluate(function, variable, point);
            } catch (ExpressionEvaluationException undefined) {
                return undefinedInside(variable, point, undefined, approximation, fApproximation, b - a,
                        iteration - 1);
            }
            double change = Math.abs(point - approximation);
            approximation = point;
            fApproximation = fPoint;
            if (fPoint == 0) {
                return RootResult.converged(point, 0.0, 0.0, iteration);
            }
            if (Math.signum(fPoint) == Math.signum(fa)) {
                a = point;
                fa = fPoint;
            } else {
                b = point;
                fb = fPoint;
            }
            if (iteration == 1 || !criteria.isWithinTolerance(change, point)) {
                continue;
            }

            double step = criteria.step(point);
            double probe = point == a ? point + step : point - step;
            if (probe <= a || probe >= b) {
                return finish(start, variable, point, fPoint, b - a, iteration);
            }
            double fProbe;
            try {
                fProbe = evaluate(function, variable, probe);
            } catch (ExpressionEvaluationException undefined) {
                return undefinedInside(variable, probe, undefined, point, fPoint, b - a, iteration);
            }
            if (fProbe == 0) {
                return RootResult.converged(probe, 0.0, 0.0, iteration);
            }
            if (Math.signum(fProbe) != Math.signum(fPoint)) {
                return finish(start, variable, point, fPoint, step, iteration);
            }
            if (point == a) {
                a = probe;
                fa = fProbe;
            } else {
                b = probe;
                fb = fProbe;
            }
            approximation = probe;
            fApproximation = fProbe;
        }
        return RootResult.iterationLimitReached(approximation, fApproximation, b - a, criteria.maxIterations());
    }

    private static double falsePositionPoint(double a, double fa, double b, double fb) {
        double weight = (fb / 2) / (fb / 2 - fa / 2);
        double point = b - weight * (b - a);
        if (point > a && point < b) {
            return point;
        }
        return a + (b - a) / 2;
    }

    private static RootResult finish(
            Bracket start, String variable, double root, double fRoot, double errorEstimate, int iterations) {
        if (Math.abs(fRoot) > start.largestEndpointValue()) {
            return RootResult.breakdown(root, fRoot, errorEstimate, iterations,
                    "The interval closed in on " + variable + " = " + root + ", but |f| there (" + Math.abs(fRoot)
                            + ") is larger than at both endpoints, so f is probably discontinuous there, not zero");
        }
        return RootResult.converged(root, fRoot, errorEstimate, iterations);
    }

    private static RootResult undefinedInside(String variable, double point, ExpressionEvaluationException undefined,
            double approximation, double fApproximation, double errorEstimate, int completedIterations) {
        return RootResult.breakdown(approximation, fApproximation, errorEstimate, completedIterations,
                "The function is undefined at " + variable + " = " + point + " (" + undefined.getMessage() + ")");
    }

    private static double evaluate(Expression function, String variable, double x) {
        return function.evaluate(Map.of(variable, x));
    }

    private record Bracket(double lower, double fLower, double upper, double fUpper) {

        static Bracket validated(
                Expression function, String variable, double lower, double upper, StoppingCriteria criteria) {
            Objects.requireNonNull(function, "function");
            Objects.requireNonNull(variable, "variable");
            Objects.requireNonNull(criteria, "criteria");
            InputValidation.requireFinite("Lower endpoint", lower);
            InputValidation.requireFinite("Upper endpoint", upper);
            if (lower >= upper) {
                throw new IllegalArgumentException("The lower endpoint must be less than the upper endpoint,"
                        + " but the interval was [" + lower + ", " + upper + "]");
            }
            if (!Double.isFinite(upper - lower)) {
                throw new IllegalArgumentException(
                        "The interval [" + lower + ", " + upper + "] is too wide to represent its length");
            }
            Bracket bracket = new Bracket(lower, endpointValue(function, variable, lower),
                    upper, endpointValue(function, variable, upper));
            if (!bracket.hasEndpointRoot() && Math.signum(bracket.fLower()) == Math.signum(bracket.fUpper())) {
                throw new IllegalArgumentException("The function has the same sign at both endpoints (f(" + lower
                        + ") = " + bracket.fLower() + ", f(" + upper + ") = " + bracket.fUpper()
                        + "), so the interval is not known to contain a root");
            }
            return bracket;
        }

        private static double endpointValue(Expression function, String variable, double x) {
            try {
                return evaluate(function, variable, x);
            } catch (ExpressionEvaluationException undefined) {
                throw new IllegalArgumentException("The function is undefined at the endpoint " + variable + " = " + x
                        + " (" + undefined.getMessage() + ")", undefined);
            }
        }

        boolean hasEndpointRoot() {
            return fLower == 0 || fUpper == 0;
        }

        RootResult endpointRoot() {
            double root = fLower == 0 ? lower : upper;
            return RootResult.converged(root, 0.0, 0.0, 0);
        }

        double betterEndpoint() {
            return Math.abs(fLower) <= Math.abs(fUpper) ? lower : upper;
        }

        double fBetterEndpoint() {
            return Math.abs(fLower) <= Math.abs(fUpper) ? fLower : fUpper;
        }

        double largestEndpointValue() {
            return Math.max(Math.abs(fLower), Math.abs(fUpper));
        }
    }
}
