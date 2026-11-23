package numericalmethods.roots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import numericalmethods.expression.ExpressionParser;
import numericalmethods.solver.SolverStatus;
import numericalmethods.solver.StoppingCriteria;
import org.junit.jupiter.api.Test;

class FixedPointIterationTest {

    private static final double ROOT_FIVE = Math.sqrt(5);
    private static final StoppingCriteria CRITERIA = StoppingCriteria.absolute(1e-10, 200);

    private static FixedPointIteration.Result run(String g) {
        return FixedPointIteration.solve(ExpressionParser.parse(g), "x", 2.5, CRITERIA);
    }

    @Test
    void steepIterationDivergesAsInTheSquareRootOfFiveExample() {
        FixedPointIteration.Result result = run("5 + x - x^2");
        assertEquals(1.25, result.iteration().iterates().get(1), 1e-12);
        assertEquals(4.6875, result.iteration().iterates().get(2), 1e-12);
        assertEquals(-12.2852, result.iteration().iterates().get(3), 1e-4);
        assertEquals(SolverStatus.BREAKDOWN, result.iteration().result().status());
    }

    @Test
    void reciprocalIterationOscillatesWithoutConverging() {
        FixedPointIteration.Result result = run("5/x");
        assertEquals(2.0, result.iteration().iterates().get(1), 1e-12);
        assertEquals(2.5, result.iteration().iterates().get(2), 1e-12);
        assertEquals(SolverStatus.ITERATION_LIMIT_REACHED, result.iteration().result().status());
        assertEquals(1.0, Math.abs(result.slopeAtEnd().orElseThrow()), 0.2);
    }

    @Test
    void slowLinearIterationConvergesWithRateNearTheDerivative() {
        FixedPointIteration.Result result = run("1 + x - x^2/5");
        assertTrue(result.iteration().result().hasConverged());
        assertEquals(ROOT_FIVE, result.iteration().result().root(), 1e-9);
        assertEquals(1 - 2 / ROOT_FIVE, result.slopeAtEnd().orElseThrow(), 1e-6);
    }

    @Test
    void newtonFormConvergesQuadratically() {
        FixedPointIteration.Result result = run("(x + 5/x)/2");
        assertEquals(2.25, result.iteration().iterates().get(1), 1e-12);
        assertEquals(ROOT_FIVE, result.iteration().result().root(), 1e-12);
        assertEquals(0.0, result.slopeAtEnd().orElseThrow(), 1e-8);
        assertTrue(result.iteration().result().iterations() < 8);
    }

    @Test
    void aitkenExtrapolationImprovesALinearlyConvergingIterate() {
        FixedPointIteration.Result shortRun = FixedPointIteration.solve(ExpressionParser.parse("1 + x - x^2/5"), "x",
                2.5, StoppingCriteria.absolute(1e-10, 3));
        double plain = shortRun.iteration().result().root();
        double extrapolated = shortRun.aitkenEstimate().orElseThrow();
        assertTrue(Math.abs(extrapolated - ROOT_FIVE) < Math.abs(plain - ROOT_FIVE));
    }

    @Test
    void intervalCheckRecognisesContraction() {
        FixedPointIteration.IntervalCheck check = FixedPointIteration.checkInterval(
                ExpressionParser.parse("1 + x - x^2/5"), "x", 2, 2.5);
        assertTrue(check.isContraction());
        assertTrue(check.lambda() < 0.25);
        FixedPointIteration.IntervalCheck steep = FixedPointIteration.checkInterval(
                ExpressionParser.parse("5 + x - x^2"), "x", 2, 2.5);
        assertFalse(steep.isContraction());
    }

    @Test
    void contractionBoundFollowsFormula() {
        assertEquals(0.5 * 0.5 / 0.5 * 0.2, FixedPointIteration.contractionBound(0.5, 2, 0.2), 1e-15);
        assertThrows(IllegalArgumentException.class, () -> FixedPointIteration.contractionBound(1, 2, 0.2));
    }

    @Test
    void cosineFixedPointConvergesToTheDottieNumber() {
        FixedPointIteration.Result result = FixedPointIteration.solve(ExpressionParser.parse("cos(x)"), "x", 1,
                new StoppingCriteria(1e-10, 200));
        assertTrue(result.iteration().result().hasConverged());
        assertEquals(0.7390851332, result.iteration().result().root(), 1e-9);
        assertTrue(Math.abs(result.slopeAtEnd().orElseThrow()) < 1);
    }

    @Test
    void exponentialFixedPointConvergesToTheOmegaConstant() {
        FixedPointIteration.Result result = FixedPointIteration.solve(ExpressionParser.parse("exp(-x)"), "x", 0.5,
                new StoppingCriteria(1e-10, 200));
        assertTrue(result.iteration().result().hasConverged());
        assertEquals(0.5671432904, result.iteration().result().root(), 1e-9);
    }

    @Test
    void newtonFormOfSquareRootOfTwoHasZeroSlopeAtTheFixedPoint() {
        FixedPointIteration.Result result = FixedPointIteration.solve(ExpressionParser.parse("(x + 2/x)/2"), "x", 1,
                new StoppingCriteria(1e-10, 200));
        assertEquals(Math.sqrt(2), result.iteration().result().root(), 1e-12);
        assertEquals(0.0, result.slopeAtEnd().orElseThrow(), 1e-8);
    }
}
