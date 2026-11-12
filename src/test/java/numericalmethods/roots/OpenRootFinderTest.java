package numericalmethods.roots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionParser;
import numericalmethods.solver.SolverStatus;
import numericalmethods.solver.StoppingCriteria;
import org.junit.jupiter.api.Test;

class OpenRootFinderTest {

    private static final Expression SIXTH_DEGREE = ExpressionParser.parse("x^6 - x - 1");
    private static final StoppingCriteria TIGHT = new StoppingCriteria(1e-10, 100);

    @Test
    void newtonReproducesKnownIterateSequenceForSixthDegreePolynomial() {
        IterativeRootResult result = OpenRootFinder.newton(SIXTH_DEGREE, "x", 1.5, TIGHT);
        List<Double> expected = List.of(1.5, 1.30049088, 1.18148042, 1.13945559, 1.13477763, 1.13472415,
                1.13472414);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), result.iterates().get(i), 1e-8);
        }
        assertEquals(SolverStatus.CONVERGED, result.result().status());
        assertEquals(1.134724138, result.result().root(), 1e-9);
    }

    @Test
    void secantReproducesKnownIterateSequence() {
        IterativeRootResult result = OpenRootFinder.secant(SIXTH_DEGREE, "x", 2, 1, TIGHT);
        List<Double> expected = List.of(2.0, 1.0, 1.01612903, 1.19057777, 1.11765583, 1.13253155, 1.13481681,
                1.13472365, 1.13472414);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), result.iterates().get(i), 1e-8);
        }
        assertTrue(result.result().hasConverged());
    }

    @Test
    void newtonObservedOrderIsAboutTwoAndSecantAboutGoldenRatio() {
        double newtonOrder = OpenRootFinder.newton(SIXTH_DEGREE, "x", 1.5, TIGHT).observedOrder().orElseThrow();
        double secantOrder = OpenRootFinder.secant(SIXTH_DEGREE, "x", 2, 1, TIGHT).observedOrder().orElseThrow();
        assertEquals(2.0, newtonOrder, 0.3);
        assertEquals(1.618, secantOrder, 0.3);
    }

    @Test
    void newtonBreaksDownOnZeroDerivative() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("x^2 + 1"), "x", 0, TIGHT);
        assertEquals(SolverStatus.BREAKDOWN, result.result().status());
        assertTrue(result.result().message().contains("derivative"));
    }

    @Test
    void newtonReportsIterationLimitWhenNoRealRootExists() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("x^2 + 1"), "x", 0.5,
                new StoppingCriteria(1e-12, 30));
        assertEquals(SolverStatus.ITERATION_LIMIT_REACHED, result.result().status());
    }

    @Test
    void newtonBreaksDownWhenIterateLeavesTheDomain() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("ln(x)"), "x", 3, TIGHT);
        assertEquals(SolverStatus.BREAKDOWN, result.result().status());
    }

    @Test
    void secantDoesNotMistakeANearlyVerticalSecantForConvergence() {
        IterativeRootResult result = OpenRootFinder.secant(ExpressionParser.parse("x^10 - 1"), "x", 0, 1.3, TIGHT);
        assertTrue(result.result().status() != SolverStatus.CONVERGED
                || Math.abs(result.result().functionValue()) < 1e-6);
    }

    @Test
    void secantRejectsEqualStartingValues() {
        assertThrows(IllegalArgumentException.class, () -> OpenRootFinder.secant(SIXTH_DEGREE, "x", 1, 1, TIGHT));
    }

    @Test
    void secantBreaksDownOnFlatSecant() {
        IterativeRootResult result = OpenRootFinder.secant(ExpressionParser.parse("x^2 - 4"), "x", -1, 1, TIGHT);
        assertEquals(SolverStatus.BREAKDOWN, result.result().status());
    }

    @Test
    void startValueOutsideDomainIsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenRootFinder.newton(ExpressionParser.parse("sqrt(x) - 1"), "x", -1, TIGHT));
    }

    @Test
    void exactRootAtStartNeedsNoIterations() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("x - 2"), "x", 2, TIGHT);
        assertEquals(0, result.result().iterations());
    }

    @Test
    void newtonDoesNotReportConvergenceOnTheTwoCycleOfCubicWithZeroOneOrbit() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("x^3 - 2*x + 2"), "x", 0,
                StoppingCriteria.defaults());
        assertEquals(SolverStatus.ITERATION_LIMIT_REACHED, result.result().status());
        assertEquals(0.0, result.iterates().get(0), 0.0);
        assertEquals(1.0, result.iterates().get(1), 1e-12);
        assertEquals(0.0, result.iterates().get(2), 1e-12);
    }

    @Test
    void newtonDoesNotReportConvergenceWhenArctangentOvershootsFromLargeStart() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("atan(x)"), "x", 1.5,
                StoppingCriteria.defaults());
        assertEquals(SolverStatus.BREAKDOWN, result.result().status());
    }

    @Test
    void newtonConvergesLinearlyToDoubleRootWithoutSignChange() {
        IterativeRootResult result = OpenRootFinder.newton(ExpressionParser.parse("(x-1)^2"), "x", 3,
                StoppingCriteria.defaults());
        assertEquals(1.0, result.result().root(), 1e-5);
    }

    @Test
    void secantFindsRootOfWallisCubic() {
        IterativeRootResult result = OpenRootFinder.secant(ExpressionParser.parse("x^3 - 2*x - 5"), "x", 2, 3,
                StoppingCriteria.defaults());
        assertTrue(result.result().hasConverged());
        assertEquals(2.0945514815, result.result().root(), 1e-9);
    }

    @Test
    void secantFromFlatRegionOfTenthPowerStallsHonestlyInsteadOfClaimingARoot() {
        IterativeRootResult result = OpenRootFinder.secant(ExpressionParser.parse("x^10 - 1"), "x", 0, 1.3,
                StoppingCriteria.defaults());
        assertEquals(SolverStatus.BREAKDOWN, result.result().status());
        assertTrue(result.result().message().contains("not a root"));
        assertEquals(-1.0, result.result().functionValue(), 1e-6);
    }
}
