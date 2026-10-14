package numericalmethods.roots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import numericalmethods.solver.SolverStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RootResultTest {

    @Test
    void convergedResultReportsAllDiagnostics() {
        RootResult result = RootResult.converged(1.5213797068, 2e-10, 5e-9, 27);

        assertTrue(result.hasConverged());
        assertEquals(SolverStatus.CONVERGED, result.status());
        assertEquals(1.5213797068, result.root());
        assertEquals(2e-10, result.functionValue());
        assertEquals(5e-9, result.errorEstimate());
        assertEquals(27, result.iterations());
        assertEquals("Converged after 27 iterations", result.message());
    }

    @Test
    void iterationLimitIsNotReportedAsConvergence() {
        RootResult result = RootResult.iterationLimitReached(1.4, -0.3, 0.1, 100);

        assertFalse(result.hasConverged());
        assertEquals(SolverStatus.ITERATION_LIMIT_REACHED, result.status());
        assertEquals("Did not converge within 100 iterations", result.message());
    }

    @Test
    void breakdownKeepsItsReason() {
        RootResult result = RootResult.breakdown(0.0, -2.0, 1.0, 3, "Derivative is zero at x = 0.0");

        assertFalse(result.hasConverged());
        assertEquals(SolverStatus.BREAKDOWN, result.status());
        assertEquals("Derivative is zero at x = 0.0", result.message());
    }

    @Test
    void exactRootBeforeAnyIterationIsAllowed() {
        RootResult result = RootResult.converged(2.0, 0.0, 0.0, 0);

        assertTrue(result.hasConverged());
        assertEquals(0, result.iterations());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
    void nonFiniteValuesAreRejected(double value) {
        assertThrows(IllegalArgumentException.class, () -> RootResult.converged(value, 0.0, 0.0, 1));
        assertThrows(IllegalArgumentException.class, () -> RootResult.converged(1.0, value, 0.0, 1));
        assertThrows(IllegalArgumentException.class, () -> RootResult.converged(1.0, 0.0, value, 1));
    }

    @Test
    void negativeErrorEstimateIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RootResult.converged(1.0, 0.0, -1e-9, 1));
    }

    @Test
    void negativeIterationCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RootResult.converged(1.0, 0.0, 0.0, -1));
    }

    @Test
    void missingStatusOrMessageIsRejected() {
        assertThrows(NullPointerException.class, () -> new RootResult(1.0, 0.0, 0.0, 1, null, "x"));
        assertThrows(NullPointerException.class,
                () -> new RootResult(1.0, 0.0, 0.0, 1, SolverStatus.CONVERGED, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void breakdownNeedsAReason(String reason) {
        assertThrows(IllegalArgumentException.class, () -> RootResult.breakdown(1.0, 0.0, 0.0, 1, reason));
    }
}
