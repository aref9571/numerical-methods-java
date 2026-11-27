package numericalmethods.cli;

import static numericalmethods.text.NumberText.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import numericalmethods.expression.Differentiator;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionFormatter;
import numericalmethods.roots.BracketingRootFinder;
import numericalmethods.roots.FixedPointIteration;
import numericalmethods.roots.IterativeRootResult;
import numericalmethods.roots.RootResult;
import numericalmethods.roots.RootScanner;
import numericalmethods.roots.OpenRootFinder;
import numericalmethods.solver.SolverStatus;
import numericalmethods.solver.StoppingCriteria;

public final class RootReports {

    private RootReports() {
    }

    public static Report compareOnInterval(Expression f, String v, double a, double b, StoppingCriteria criteria) {
        Report report = new Report("Root finding: all methods on [" + format(a) + ", " + format(b) + "]");
        echo(report, f, v, criteria);
        report.input("interval: [" + format(a) + ", " + format(b) + "]");

        List<Named> runs = new ArrayList<>();
        runs.add(new Named("Bisection", BracketingRootFinder.bisection(f, v, a, b, criteria), OptionalDouble.empty()));
        runs.add(new Named("False position", BracketingRootFinder.falsePosition(f, v, a, b, criteria),
                OptionalDouble.empty()));
        double middle = a + (b - a) / 2;
        Expression derivative = Differentiator.differentiate(f, v);
        IterativeRootResult newton = safeNewton(f, derivative, v, middle, criteria);
        if (newton != null) {
            runs.add(new Named("Newton (from " + format(middle) + ")", newton.result(), newton.observedOrder()));
        }
        IterativeRootResult secant = safeSecant(f, v, a, b, criteria);
        if (secant != null) {
            runs.add(new Named("Secant (from " + format(a) + ", " + format(b) + ")", secant.result(),
                    secant.observedOrder()));
        }

        TextTable table = new TextTable("method", v, "f(" + v + ")", "iterations", "status", "order");
        for (Named run : runs) {
            table.row(run.name(), format(run.result().root()), format(run.result().functionValue()),
                    Integer.toString(run.result().iterations()), status(run.result().status()),
                    run.order().isPresent() ? format(run.order().getAsDouble(), 3) : "-");
        }
        report.table("Comparison", table.lines());

        Named primary = runs.get(0);
        for (Named run : runs) {
            if (run.result().hasConverged() && inside(run.result().root(), a, b)
                    && (!primary.result().hasConverged()
                    || Math.abs(run.result().functionValue()) < Math.abs(primary.result().functionValue()))) {
                primary = run;
            }
        }
        if (primary.result().hasConverged()) {
            report.result(v + " = " + format(primary.result().root()) + "   (from " + primary.name()
                    + ", the most accurate converged result inside the interval)");
            report.diagnostic("f(" + v + ") = " + format(primary.result().functionValue()));
            report.diagnostic("status: " + status(primary.result().status()));
        } else {
            report.result("no root found: no method converged (see the warnings)");
        }
        for (Named run : runs) {
            if (run.result().hasConverged() && !inside(run.result().root(), a, b)) {
                report.warning(run.name() + " converged to " + format(run.result().root())
                        + ", which is outside the interval");
            }
            if (run.result().status() != SolverStatus.CONVERGED) {
                report.warning(run.name() + ": " + run.result().message());
            }
        }
        if (primary.result().hasConverged()) {
            addOtherRoots(report, f, v, a, b, criteria, primary.result().root());
        }
        return report;
    }

    public static Report bracketing(String method, Expression f, String v, double a, double b,
            StoppingCriteria criteria) {
        boolean bisection = method.equals("bisection");
        RootResult result = bisection ? BracketingRootFinder.bisection(f, v, a, b, criteria)
                : BracketingRootFinder.falsePosition(f, v, a, b, criteria);
        Report report = new Report("Root finding: " + (bisection ? "bisection" : "false position (regula falsi)"));
        echo(report, f, v, criteria);
        report.input("interval: [" + format(a) + ", " + format(b) + "]");
        describe(report, v, result, "bracket width with a sign change");
        if (result.hasConverged()) {
            addOtherRoots(report, f, v, a, b, criteria, result.root());
        }
        return report;
    }

    public static Report newton(Expression f, String v, double start, StoppingCriteria criteria) {
        Expression derivative = Differentiator.differentiate(f, v);
        IterativeRootResult run = OpenRootFinder.newton(f, derivative, v, start, criteria);
        Report report = new Report("Root finding: Newton's method");
        echo(report, f, v, criteria);
        report.input("f'(" + v + ") = " + ExpressionFormatter.format(derivative));
        report.input("start: " + v + "0 = " + format(start));
        describe(report, v, run.result(), "last step |x(n+1) - x(n)|");
        return report;
    }

    public static Report secant(Expression f, String v, double first, double second, StoppingCriteria criteria) {
        IterativeRootResult run = OpenRootFinder.secant(f, v, first, second, criteria);
        Report report = new Report("Root finding: secant method");
        echo(report, f, v, criteria);
        report.input("starts: " + v + "0 = " + format(first) + ", " + v + "1 = " + format(second));
        describe(report, v, run.result(), "last step |x(n+1) - x(n)|");
        return report;
    }

    public static Report fixedPoint(Expression g, String v, double start, StoppingCriteria criteria,
            double[] interval) {
        FixedPointIteration.Result run = FixedPointIteration.solve(g, v, start, criteria);
        RootResult result = run.iteration().result();
        Report report = new Report("Root finding: fixed-point iteration x = g(x)");
        report.input("g(" + v + ") = " + ExpressionFormatter.format(g));
        report.input("g'(" + v + ") = " + ExpressionFormatter.format(run.derivative()));
        report.input("start: " + v + "0 = " + format(start) + ", tolerance " + format(criteria.tolerance())
                + ", max " + criteria.maxIterations() + " iterations");
        report.result(v + " = " + format(result.root()));
        report.diagnostic("g(" + v + ") - " + v + " = " + format(result.functionValue()));
        report.diagnostic("iterations: " + result.iterations());
        report.diagnostic("last step: " + format(result.errorEstimate()));
        report.diagnostic(statusLine(result));
        run.slopeAtEnd().ifPresent(slope -> {
            if (Math.abs(slope) > 1.0001) {
                report.warning("|g'| > 1 near the iterate, so the iteration does not converge to this point");
            } else if (Math.abs(Math.abs(slope) - 1) <= 1e-4) {
                report.warning("|g'| = 1 near the iterate: no conclusion about convergence");
            }
        });
        List<Double> iterates = run.iteration().iterates();
        int n = iterates.size() - 1;
        double lastStep = n >= 1 ? Math.abs(iterates.get(n) - iterates.get(n - 1)) : 0;
        double previousStep = n >= 2 ? Math.abs(iterates.get(n - 1) - iterates.get(n - 2)) : 0;
        double ratio = previousStep > 0 ? lastStep / previousStep : Double.NaN;
        boolean persistentCycle = false;
        if (!result.hasConverged() && n >= 3) {
            double last = iterates.get(n);
            double before = iterates.get(n - 1);
            double scale = 1e-6 * Math.max(1, Math.abs(last));
            if (Math.abs(last - iterates.get(n - 2)) <= scale && Math.abs(last - before) > scale
                    && ratio >= 0.999) {
                persistentCycle = true;
                double middle = (last + before) / 2;
                String slopeText = "";
                try {
                    slopeText = "; g'(" + format(middle) + ") = " + format(
                            run.derivative().evaluate(Map.of(v, middle)), 4)
                            + " between them, so |g'(alpha)| is about 1";
                } catch (RuntimeException undefined) {
                    slopeText = "";
                }
                report.warning("the iterates alternate between " + format(before) + " and " + format(last)
                        + " (a 2-cycle), so the iteration cannot converge" + slopeText);
            }
        }
        if (!persistentCycle && Double.isFinite(ratio) && ratio > 0 && ratio < 1 && n >= 3) {
            double remaining = lastStep * ratio / (1 - ratio);
            if (!result.hasConverged() && result.status() == SolverStatus.ITERATION_LIMIT_REACHED && ratio > 0.5) {
                int needed = (int) Math.ceil(Math.log(criteria.tolerance() / Math.max(lastStep, 1e-300))
                        / Math.log(ratio));
                report.warning("the steps shrink only by the factor " + format(ratio, 5) + " per iteration (slow"
                        + " convergence, not a cycle); about " + Math.max(needed, 1) + " more iterations would be"
                        + " needed: increase max");
            } else if (result.hasConverged() && remaining > 10 * criteria.tolerance()) {
                report.warning("the last steps shrink only by the factor " + format(ratio, 5) + ", so the real error"
                        + " is about step*r/(1-r) = " + format(remaining) + ", much larger than the tolerance "
                        + format(criteria.tolerance()) + " (a small step does not mean a small error when g' is"
                        + " close to 1)");
            }
        }
        run.aitkenEstimate().ifPresent(estimate -> {
            if (!result.hasConverged() || run.aitkenErrorEstimate().isPresent()
                    && Math.abs(run.aitkenErrorEstimate().getAsDouble()) > 10 * criteria.tolerance()) {
                report.diagnostic("Aitken extrapolation of the last three iterates: " + v + " ~ " + format(estimate)
                        + " (estimated error of the last iterate "
                        + format(Math.abs(run.aitkenErrorEstimate().orElse(Double.NaN))) + ")");
            }
        });
        if (interval != null) {
            FixedPointIteration.IntervalCheck check = FixedPointIteration.checkInterval(g, v, interval[0],
                    interval[1]);
            if (check.isContraction() && run.iteration().iterates().size() > 1) {
                double firstStep = run.iteration().iterates().get(1) - start;
                report.diagnostic("contraction on [" + format(interval[0]) + ", " + format(interval[1])
                        + "]: lambda = max|g'| = " + format(check.lambda()) + ", guaranteed error bound "
                        + format(FixedPointIteration.contractionBound(check.lambda(), result.iterations(), firstStep)));
            } else {
                report.warning("the contraction conditions do not hold on this interval, so convergence is not"
                        + " guaranteed");
            }
            if (result.hasConverged() && (result.root() < interval[0] || result.root() > interval[1])) {
                report.warning("the limit " + format(result.root()) + " lies outside the interval ["
                        + format(interval[0]) + ", " + format(interval[1]) + "]");
            }
        }
        return report;
    }

    public static Report scan(Expression f, String v, double a, double b, int samples, StoppingCriteria criteria) {
        RootScanner.ScanResult scan = RootScanner.scan(f, v, a, b, samples, criteria);
        Report report = new Report("Root finding: all roots in [" + format(a) + ", " + format(b) + "]");
        echo(report, f, v, criteria);
        report.input("interval: [" + format(a) + ", " + format(b) + "], " + samples + " samples");
        if (scan.roots().isEmpty()) {
            report.result("no sign change found, so no root was located");
        } else {
            report.result(scan.roots().size() + " root(s) found");
            TextTable table = new TextTable("#", v, "f(" + v + ")");
            int index = 1;
            for (RootResult root : scan.roots()) {
                table.row(Integer.toString(index++), format(root.root()), format(root.functionValue()));
            }
            report.table("Roots", table.lines());
        }
        for (double point : scan.suspectedDiscontinuities()) {
            report.warning("sign change near " + v + " = " + format(point) + " looks like a pole or jump, not a root");
        }
        if (scan.undefinedSamples() > 0) {
            report.warning("f is undefined at " + scan.undefinedSamples() + " sample point(s)");
        }
        double spacing = (b - a) / samples;
        if (scan.identicallyZeroSamples() > 0.5 * samples) {
            report.warning("f is exactly 0 at " + scan.identicallyZeroSamples() + " of the sample points: f is"
                    + " identically zero (or zero on a whole interval), so there are no isolated roots to report");
        } else if (scan.roots().size() + scan.suspectedDiscontinuities().size() >= 0.1 * samples) {
            report.warning("f changes sign in " + (scan.roots().size() + scan.suspectedDiscontinuities().size())
                    + " of the " + samples + " sample intervals (spacing " + format(spacing) + "): the samples are"
                    + " probably too far apart for this function, so roots are missed and sign changes may be"
                    + " misread; increase samples");
        }
        for (double dip : scan.possibleDoubleRoots()) {
            report.warning("|f| dips sharply near " + v + " = " + format(dip) + " without a sign change: a double"
                    + " root (touching zero) or two roots closer together than the spacing " + format(spacing)
                    + " may be hiding there; check with 'newton' or more samples");
        }
        return report;
    }

    private static void addOtherRoots(Report report, Expression f, String v, double a, double b,
            StoppingCriteria criteria, double found) {
        try {
            RootScanner.ScanResult scan = RootScanner.scan(f, v, a, b, RootScanner.DEFAULT_SAMPLES, criteria);
            List<String> others = new ArrayList<>();
            for (RootResult root : scan.roots()) {
                if (Math.abs(root.root() - found) > 1e3 * criteria.tolerance() * Math.max(1, Math.abs(found))) {
                    others.add(format(root.root()));
                }
            }
            if (!others.isEmpty()) {
                report.warning("the interval also contains other roots at " + String.join(", ", others)
                        + "; the result is only one of them");
            }
        } catch (RuntimeException scanNotPossible) {
            return;
        }
    }

    private static void echo(Report report, Expression f, String v, StoppingCriteria criteria) {
        report.input("f(" + v + ") = " + ExpressionFormatter.format(f));
        report.input("tolerance " + format(criteria.tolerance()) + " (" + criteria.mode().name().toLowerCase()
                + "), max " + criteria.maxIterations() + " iterations");
    }

    private static void describe(Report report, String v, RootResult result, String errorMeaning) {
        report.result(v + " = " + format(result.root()));
        report.diagnostic("f(" + v + ") = " + format(result.functionValue()));
        report.diagnostic("iterations: " + result.iterations());
        report.diagnostic("error estimate (" + errorMeaning + "): " + format(result.errorEstimate()));
        report.diagnostic(statusLine(result));
        if (result.status() != SolverStatus.CONVERGED) {
            report.warning("the result did not converge; the value above is only the last approximation");
        }
    }

    private static String statusLine(RootResult result) {
        return result.hasConverged() ? "status: converged"
                : "status: " + status(result.status()) + " - " + result.message();
    }

    private static IterativeRootResult safeNewton(Expression f, Expression derivative, String v, double start,
            StoppingCriteria criteria) {
        try {
            return OpenRootFinder.newton(f, derivative, v, start, criteria);
        } catch (IllegalArgumentException undefinedStart) {
            return null;
        }
    }

    private static IterativeRootResult safeSecant(Expression f, String v, double a, double b,
            StoppingCriteria criteria) {
        try {
            return OpenRootFinder.secant(f, v, a, b, criteria);
        } catch (IllegalArgumentException undefinedStart) {
            return null;
        }
    }

    private static boolean inside(double x, double a, double b) {
        return x >= a && x <= b;
    }

    static String status(SolverStatus status) {
        return switch (status) {
            case CONVERGED -> "converged";
            case ITERATION_LIMIT_REACHED -> "iteration limit reached";
            case BREAKDOWN -> "breakdown";
        };
    }

    private record Named(String name, RootResult result, OptionalDouble order) {
    }
}
