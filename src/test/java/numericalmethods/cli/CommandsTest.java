package numericalmethods.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandsTest {

    private static String run(String command) {
        return Commands.execute(command).render();
    }

    private static void assertContains(String text, String... parts) {
        for (String part : parts) {
            assertTrue(text.contains(part), () -> "missing '" + part + "' in:\n" + text);
        }
    }

    private static void assertNoWarnings(String command) {
        Report report = Commands.execute(command);
        assertTrue(report.lines(Report.WARNINGS).isEmpty(), () -> command + " warned:\n" + report.render());
    }

    @Test
    void rootComparisonShowsEveryMethodAndThePrimaryResult() {
        Report report = Commands.execute("root x^6 - x - 1 on [1, 2], tol 0.001");
        String text = report.render();
        assertContains(text, "Bisection", "False position", "Newton", "Secant", "1.1337");
        assertTrue(report.lines(Report.RESULT).get(0).startsWith("x = 1.1347"));
        assertEquals(List.of(Report.INPUT, Report.RESULT, Report.DIAGNOSTICS, "Comparison"), report.sectionNames());
    }

    @Test
    void bisectionReportsIterationsAndErrorEstimate() {
        assertContains(run("bisection x^6 - x - 1 on [1, 2], tol 0.001"), "x = 1.133789063", "iterations: 10",
                "0.0009765625", "status: converged", "tolerance 0.001 (absolute)");
    }

    @Test
    void rootAtAnEndpointNeedsNoIterations() {
        assertContains(run("bisection x^2 - 4 on [2, 5]"), "x = 2", "iterations: 0");
    }

    @Test
    void openMethodsAndFixedPoint() {
        assertContains(run("newton x^6 - x - 1 from 1.5"), "x = 1.134724138", "f'(x)");
        assertContains(run("root x^6 - x - 1 from 1.5"), "Newton's method", "x = 1.134724138");
        assertContains(run("secant x^6 - x - 1 from [2, 1]"), "x = 1.134724138");
        assertContains(run("fixedpoint (x + 5/x)/2 from 2.5, interval [2, 3]"), "x = 2.236067977", "contraction");
        assertContains(run("fixedpoint 5 + x - x^2 from 2.5"), "breakdown", "|g'| > 1");
    }

    @Test
    void scanFindsSeveralRoots() {
        assertContains(run("scan sin(10*x) on [0, 3]"), "10 root(s) found");
        assertContains(run("scan sin(1/x) on [0.05, 1]"), "6 root(s) found", "0.3183098862", "0.05305164");
    }

    @Test
    void poleIsNotReportedAsRoot() {
        Report report = Commands.execute("root 1/(x-1) on [0, 2]");
        assertTrue(report.lines(Report.RESULT).get(0).startsWith("no root found"));
    }

    @Test
    void scanWarnsAboutAPoleButNotAboutOrdinaryRoots() {
        assertContains(run("scan 1/(x - 1.05) on [0, 2], samples 100"), "looks like a pole or jump");
        assertNoWarnings("scan sin(10*x) on [0, 3]");
    }

    @Test
    void scanWarnsAboutATouchingRootButNotAboutSimpleRoots() {
        assertContains(run("scan (x - 1.01)^2 on [0, 3], samples 100"), "no sign change found", "dips sharply");
        assertNoWarnings("scan x^6 - x - 1 on [-2, 2], samples 400");
    }

    @Test
    void bracketingWarnsAboutOtherRootsOnlyWhenThereAreSome() {
        assertContains(run("bisection sin(10*x) on [0.1, 1]"), "also contains other roots");
        assertNoWarnings("bisection x^6 - x - 1 on [1, 2], tol 0.001");
    }

    @Test
    void fixedPointWarnsAboutATwoCycleButNotAboutSlowConvergence() {
        assertContains(run("fixedpoint 5/x from 2.5"), "2-cycle", "cannot converge");
        assertNoWarnings("fixedpoint cos(x) from 1");
    }

    @Test
    void fixedPointWarnsWhenTheIterationLimitCutsOffSlowConvergence() {
        assertContains(run("fixedpoint cos(x) from 1, max 20"), "shrink only by the factor", "increase max");
    }

    @Test
    void fixedPointContractionCheckWarnsOnlyWhenTheConditionsFail() {
        assertContains(run("fixedpoint 5 + x - x^2 from 2.5, interval [2, 3]"), "contraction conditions do not hold");
        assertNoWarnings("fixedpoint 1 + x - x^2/5 from 2.5, interval [2, 2.5]");
    }

    @Test
    void convergingTextbookExamplesProduceNoWarnings() {
        assertNoWarnings("newton x^6 - x - 1 from 1.5");
        assertNoWarnings("secant x^6 - x - 1 from [2, 1]");
        assertNoWarnings("fixedpoint (x + 2/x)/2 from 1");
    }

    @Test
    void nonConvergedResultsAreNotPresentedAsRoots() {
        assertContains(run("secant x^10 - 1 from [0, 1.3]"), "not a root", "breakdown",
                "only the last approximation");
        assertContains(run("newton x^3 - 2*x + 2 from 0"), "iteration limit reached");
        assertContains(run("newton atan(x) from 1.5"), "breakdown");
    }

    @Test
    void constantExpressionsAreAcceptedAsNumbers() {
        assertContains(run("bisection sin(x) on [pi/2, 3*pi/2]"), "x = 3.141592654");
        assertContains(run("newton x^2 - 2 from 1, tol 1e-6, max 2*10"), "tolerance 1e-6 (absolute), max 20");
    }

    @ParameterizedTest
    @ValueSource(strings = {"root 2x on [0, 1]", "root x on [0]", "root x on [0, 1], speed 3", "frobnicate x y",
            "root x^2 + 1 on [-1, 1]", "root x", "bisection x on [0, a]", "newton x*y from 1",
            "secant x from [1]", "fixedpoint x from 1, interval [1]", "root x on [0, 1], tol 0",
            "root x on [0, 1], max 1.5", "root x on [0, 1], tol", "root x on [0, 1], tol 1, tol 2", "help"})
    void invalidInputGivesAClearError(String command) {
        RuntimeException error = assertThrows(RuntimeException.class, () -> run(command));
        assertFalse(error.getMessage() == null || error.getMessage().isBlank());
    }

    @Test
    void splitTopLevelRespectsBrackets() {
        assertEquals(List.of("fixedpoint g from 1", "interval [2, 3]", "tol 1e-6"),
                Inputs.splitTopLevel("fixedpoint g from 1, interval [2, 3], tol 1e-6", ','));
        assertEquals(List.of("root atan2(x, 1)", "max 5"), Inputs.splitTopLevel("root atan2(x, 1), max 5", ','));
    }

    @Test
    void helpMentionsEveryRootCommand() {
        String help = Commands.help();
        for (String keyword : List.of("root", "bisection", "falseposition", "scan", "newton", "secant",
                "fixedpoint")) {
            assertContains(help, keyword);
        }
    }
}
