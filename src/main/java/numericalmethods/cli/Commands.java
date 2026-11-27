package numericalmethods.cli;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionParser;
import numericalmethods.roots.RootScanner;
import numericalmethods.solver.StoppingCriteria;

public final class Commands {

    public static final double DEFAULT_TOLERANCE = 1e-10;
    public static final int DEFAULT_MAX_ITERATIONS = 100;

    private static final Pattern ON_INTERVAL = Pattern.compile("^(.+?)\\s+on\\s+\\[(.+),(.+)]$");
    private static final Pattern FROM = Pattern.compile("^(.+?)\\s+from\\s+(.+)$");
    private static final Set<String> ROOT_OPTIONS = Set.of("tol", "max", "samples", "interval");
    private static final Set<String> KEYWORDS = Set.of("root", "bisection", "falseposition", "scan", "newton",
            "secant", "fixedpoint");

    private Commands() {
    }

    public static String help() {
        return String.join(System.lineSeparator(),
                "One-line commands (options go after commas, e.g. ', tol 1e-6'):",
                "  root x^6 - x - 1 on [1, 2]              all root methods compared",
                "  bisection f on [a, b]   falseposition f on [a, b]   scan f on [a, b]",
                "  newton x^6 - x - 1 from 1.5             secant f from [2, 1]",
                "  fixedpoint (x + 5/x)/2 from 2.5, interval [2, 3]",
                "Options: tol (absolute tolerance, default 1e-10), max (iterations, default 100),"
                        + " samples (scan, default 1000), interval (fixedpoint contraction check)",
                "Functions: sin cos tan asin acos atan exp ln log10 sqrt abs; constants pi e; write 2*x, not 2x.");
    }

    public static Report execute(String line) {
        String trimmed = line.trim();
        int space = trimmed.indexOf(' ');
        String keyword = (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase(Locale.ROOT);
        if (space < 0 || !KEYWORDS.contains(keyword)) {
            throw new IllegalArgumentException("Unknown command '" + keyword + "'; type 'help' for examples");
        }
        List<String> parts = Inputs.splitTopLevel(trimmed.substring(space + 1).trim(), ',');
        String main = parts.get(0);
        List<String> rest = parts.subList(1, parts.size());
        return switch (keyword) {
            case "root" -> root(main, rest);
            case "bisection", "falseposition" -> bracketing(keyword, main, rest);
            case "scan" -> scan(main, rest);
            case "newton" -> newton(main, rest);
            case "secant" -> secant(main, rest);
            case "fixedpoint" -> fixedPoint(main, rest);
            default -> throw new IllegalArgumentException("Unknown command '" + keyword
                    + "'; type 'help' for examples");
        };
    }

    private static Report root(String main, List<String> rest) {
        Matcher interval = ON_INTERVAL.matcher(main);
        if (interval.matches()) {
            Expression f = ExpressionParser.parse(interval.group(1));
            Options options = Options.parse(rest, ROOT_OPTIONS);
            return RootReports.compareOnInterval(f, Inputs.singleVariable(f, "x"), Inputs.number(interval.group(2)),
                    Inputs.number(interval.group(3)), criteria(options));
        }
        Matcher from = FROM.matcher(main);
        if (from.matches()) {
            return newton(main, rest);
        }
        throw new IllegalArgumentException("Write 'root <f> on [a, b]' or 'root <f> from x0'");
    }

    private static Report bracketing(String keyword, String main, List<String> rest) {
        Matcher interval = requireMatch(ON_INTERVAL, main, keyword + " <f> on [a, b]");
        Expression f = ExpressionParser.parse(interval.group(1));
        return RootReports.bracketing(keyword, f, Inputs.singleVariable(f, "x"), Inputs.number(interval.group(2)),
                Inputs.number(interval.group(3)), criteria(Options.parse(rest, ROOT_OPTIONS)));
    }

    private static Report scan(String main, List<String> rest) {
        Matcher interval = requireMatch(ON_INTERVAL, main, "scan <f> on [a, b]");
        Expression f = ExpressionParser.parse(interval.group(1));
        Options options = Options.parse(rest, ROOT_OPTIONS);
        return RootReports.scan(f, Inputs.singleVariable(f, "x"), Inputs.number(interval.group(2)),
                Inputs.number(interval.group(3)), options.integer("samples", RootScanner.DEFAULT_SAMPLES),
                criteria(options));
    }

    private static Report newton(String main, List<String> rest) {
        Matcher from = requireMatch(FROM, main, "newton <f> from x0");
        Expression f = ExpressionParser.parse(from.group(1));
        return RootReports.newton(f, Inputs.singleVariable(f, "x"), Inputs.number(from.group(2)),
                criteria(Options.parse(rest, ROOT_OPTIONS)));
    }

    private static Report secant(String main, List<String> rest) {
        Matcher from = requireMatch(FROM, main, "secant <f> from [x0, x1]");
        Expression f = ExpressionParser.parse(from.group(1));
        double[] starts = Inputs.vector(from.group(2));
        if (starts.length != 2) {
            throw new IllegalArgumentException("The secant method needs two starting values, e.g. from [2, 1]");
        }
        return RootReports.secant(f, Inputs.singleVariable(f, "x"), starts[0], starts[1],
                criteria(Options.parse(rest, ROOT_OPTIONS)));
    }

    private static Report fixedPoint(String main, List<String> rest) {
        Matcher from = requireMatch(FROM, main, "fixedpoint <g> from x0");
        Expression g = ExpressionParser.parse(from.group(1));
        Options options = Options.parse(rest, ROOT_OPTIONS);
        double[] interval = options.text("interval").map(Inputs::vector).orElse(null);
        if (interval != null && interval.length != 2) {
            throw new IllegalArgumentException("The interval needs two numbers, e.g. interval [2, 3]");
        }
        return RootReports.fixedPoint(g, Inputs.singleVariable(g, "x"), Inputs.number(from.group(2)),
                criteria(options), interval);
    }

    private static StoppingCriteria criteria(Options options) {
        return StoppingCriteria.absolute(options.number("tol", DEFAULT_TOLERANCE),
                options.integer("max", DEFAULT_MAX_ITERATIONS));
    }

    private static Matcher requireMatch(Pattern pattern, String text, String usage) {
        Matcher matcher = pattern.matcher(text.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Could not read '" + text.trim() + "'; write: " + usage);
        }
        return matcher;
    }
}
