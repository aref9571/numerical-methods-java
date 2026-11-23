package numericalmethods.roots;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionEvaluationException;
import numericalmethods.solver.InputValidation;
import numericalmethods.solver.SolverStatus;
import numericalmethods.solver.StoppingCriteria;

public final class RootScanner {

    public static final int DEFAULT_SAMPLES = 1000;
    public static final int MAX_SAMPLES = 1_000_000;

    private RootScanner() {
    }

    public record ScanResult(List<RootResult> roots, List<Double> suspectedDiscontinuities, int undefinedSamples,
            List<Double> possibleDoubleRoots, int identicallyZeroSamples) {

        public ScanResult {
            roots = List.copyOf(roots);
            suspectedDiscontinuities = List.copyOf(suspectedDiscontinuities);
            possibleDoubleRoots = List.copyOf(possibleDoubleRoots);
        }
    }

    public static ScanResult scan(Expression function, String variable, double lower, double upper, int samples,
            StoppingCriteria criteria) {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(variable, "variable");
        Objects.requireNonNull(criteria, "criteria");
        InputValidation.requireFinite("Lower endpoint", lower);
        InputValidation.requireFinite("Upper endpoint", upper);
        InputValidation.requireInRange("Samples", samples, 2, MAX_SAMPLES);
        if (lower >= upper) {
            throw new IllegalArgumentException("The lower endpoint must be less than the upper endpoint");
        }
        List<RootResult> roots = new ArrayList<>();
        List<Double> discontinuities = new ArrayList<>();
        int undefined = 0;
        int zeros = 0;
        List<Double> dips = new ArrayList<>();
        double previousX = Double.NaN;
        double previousF = Double.NaN;
        double beforePreviousF = Double.NaN;
        for (int i = 0; i <= samples; i++) {
            double x = i == samples ? upper : lower + (upper - lower) * i / samples;
            double fx;
            try {
                fx = OpenRootFinder.evaluate(function, variable, x);
            } catch (ExpressionEvaluationException notDefined) {
                undefined++;
                previousX = Double.NaN;
                previousF = Double.NaN;
                continue;
            }
            if (Double.isFinite(beforePreviousF) && Double.isFinite(previousF) && beforePreviousF != 0
                    && previousF != 0 && fx != 0 && Math.signum(beforePreviousF) == Math.signum(previousF)
                    && Math.signum(previousF) == Math.signum(fx)
                    && Math.abs(previousF) <= 0.25 * Math.min(Math.abs(beforePreviousF), Math.abs(fx))) {
                dips.add(previousX);
            }
            if (fx == 0) {
                zeros++;
                roots.add(RootResult.converged(x, 0.0, 0.0, 0));
            } else if (!Double.isNaN(previousF) && previousF != 0 && Math.signum(previousF) != Math.signum(fx)) {
                RootResult refined = BracketingRootFinder.bisection(function, variable, previousX, x, criteria);
                if (refined.status() == SolverStatus.BREAKDOWN) {
                    discontinuities.add(refined.root());
                } else {
                    roots.add(refined);
                }
            }
            beforePreviousF = previousF;
            previousX = x;
            previousF = fx;
        }
        return new ScanResult(roots, discontinuities, undefined, dips, zeros);
    }

    public static int predictedBisectionIterations(double lower, double upper, double tolerance) {
        InputValidation.requirePositiveFinite("Tolerance", tolerance);
        double width = upper - lower;
        if (!(width > 0) || !Double.isFinite(width)) {
            throw new IllegalArgumentException("The interval must have a positive finite width");
        }
        return (int) Math.max(0, Math.ceil((Math.log(width) - Math.log(tolerance)) / Math.log(2)));
    }

    public static double bisectionBound(double lower, double upper, int iterations) {
        return (upper - lower) / Math.pow(2, iterations);
    }
}
