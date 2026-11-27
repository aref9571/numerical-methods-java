package numericalmethods.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import numericalmethods.cli.App.MenuItem;
import numericalmethods.cli.App.Prompt;
import numericalmethods.cli.App.Topic;

final class Menus {

    private static final Prompt FUNCTION = new Prompt("f(x)", "x^6 - x - 1");
    private static final Prompt LOWER = new Prompt("Left end a", "1");
    private static final Prompt UPPER = new Prompt("Right end b", "2");
    private static final Prompt TOLERANCE = new Prompt("Tolerance", "1e-10");
    private static final Prompt MAX = new Prompt("Maximum iterations", "100");

    private Menus() {
    }

    static List<Topic> topics() {
        return List.of(
                new Topic("Root finding", List.of(
                        item("Compare all methods on an interval", a -> command("root " + a.get(0) + " on [" + a.get(1)
                                + ", " + a.get(2) + "]", "tol", a.get(3), "max", a.get(4)),
                                FUNCTION, LOWER, UPPER, TOLERANCE, MAX),
                        item("Bisection", a -> command("bisection " + a.get(0) + " on [" + a.get(1) + ", " + a.get(2)
                                + "]", "tol", a.get(3), "max", a.get(4)),
                                FUNCTION, LOWER, UPPER, p("Tolerance", "0.001"), MAX),
                        item("False position (regula falsi)", a -> command("falseposition " + a.get(0) + " on ["
                                + a.get(1) + ", " + a.get(2) + "]", "tol", a.get(3), "max", a.get(4)),
                                FUNCTION, LOWER, UPPER, TOLERANCE, MAX),
                        item("Newton's method", a -> command("newton " + a.get(0) + " from " + a.get(1),
                                "tol", a.get(2), "max", a.get(3)),
                                FUNCTION, p("Starting value x0", "1.5"), TOLERANCE, MAX),
                        item("Secant method", a -> command("secant " + a.get(0) + " from [" + a.get(1) + ", "
                                + a.get(2) + "]", "tol", a.get(3), "max", a.get(4)),
                                FUNCTION, p("x0", "2"), p("x1", "1"), TOLERANCE, MAX),
                        item("Fixed-point iteration x = g(x)", a -> command("fixedpoint " + a.get(0) + " from "
                                + a.get(1), "interval", a.get(2).isEmpty() ? "" : "[" + a.get(2) + "]",
                                "tol", a.get(3), "max", a.get(4)),
                                p("g(x)", "1 + x - x^2/5"), p("Starting value x0", "2.5"),
                                p("Interval for the contraction check, e.g. 2, 3", "2, 2.5"), TOLERANCE, MAX),
                        item("Find all roots in an interval", a -> command("scan " + a.get(0) + " on [" + a.get(1)
                                + ", " + a.get(2) + "]", "samples", a.get(3)),
                                p("f(x)", "sin(10*x)"), p("Left end a", "0"), p("Right end b", "3"),
                                p("Samples", "1000")))));
    }

    private static MenuItem item(String label, Function<List<String>, String> command, Prompt... prompts) {
        return new MenuItem(label, List.of(prompts), command);
    }

    private static Prompt p(String question, String defaultValue) {
        return new Prompt(question, defaultValue);
    }

    private static String command(String base, String... optionPairs) {
        List<String> parts = new ArrayList<>(List.of(base));
        for (int i = 0; i < optionPairs.length; i += 2) {
            if (!optionPairs[i + 1].isBlank()) {
                parts.add(optionPairs[i] + " " + optionPairs[i + 1]);
            }
        }
        return String.join(", ", parts);
    }
}
