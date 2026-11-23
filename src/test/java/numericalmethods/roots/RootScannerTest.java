package numericalmethods.roots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import numericalmethods.expression.ExpressionParser;
import numericalmethods.solver.StoppingCriteria;
import org.junit.jupiter.api.Test;

class RootScannerTest {

    private static final StoppingCriteria CRITERIA = StoppingCriteria.absolute(1e-12, 200);

    @Test
    void findsEveryRootOfAnOscillatingFunction() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("sin(10*x)"), "x", 0, 3, 1000,
                CRITERIA);
        assertEquals(10, result.roots().size());
        for (int k = 0; k < 10; k++) {
            assertEquals(k * Math.PI / 10, result.roots().get(k).root(), 1e-10);
        }
    }

    @Test
    void reportsPoleAsSuspectedDiscontinuityNotAsRoot() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("1/(x - 1.05)"), "x", 0, 2, 100,
                CRITERIA);
        assertTrue(result.roots().isEmpty());
        assertEquals(1, result.suspectedDiscontinuities().size());
    }

    @Test
    void skipsUndefinedSamples() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("ln(x) - 0.5"), "x", -1, 3, 400,
                CRITERIA);
        assertEquals(1, result.roots().size());
        assertEquals(Math.sqrt(Math.E), result.roots().get(0).root(), 1e-10);
        assertTrue(result.undefinedSamples() > 0);
    }

    @Test
    void predictsBisectionIterationCountFromTheErrorBound() {
        assertEquals(10, RootScanner.predictedBisectionIterations(1, 2, 0.001));
        assertEquals(1.0 / 1024, RootScanner.bisectionBound(1, 2, 10), 1e-15);
        assertThrows(IllegalArgumentException.class, () -> RootScanner.predictedBisectionIterations(2, 1, 0.1));
    }

    @Test
    void bisectionMatchesKnownTableForSixthDegreePolynomial() {
        RootResult result = BracketingRootFinder.bisection(ExpressionParser.parse("x^6 - x - 1"), "x", 1, 2,
                StoppingCriteria.absolute(0.001, 100));
        assertEquals(10, result.iterations());
        assertEquals(1.1337890625, result.root(), 1e-12);
    }

    @Test
    void falsePositionWithAbsoluteToleranceKeepsErrorEstimateBelowTolerance() {
        RootResult result = BracketingRootFinder.falsePosition(ExpressionParser.parse("x^6 - x - 1"), "x", 1, 2,
                StoppingCriteria.absolute(0.001, 100));
        assertTrue(result.hasConverged());
        assertTrue(result.errorEstimate() <= 0.001);
        assertEquals(1.134724138, result.root(), 0.001);
    }

    @Test
    void findsTheRootsOfSineOfOneOverXThatCrowdTowardsZero() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("sin(1/x)"), "x", 0.05, 1, 1000,
                CRITERIA);
        assertEquals(6, result.roots().size());
        for (int k = 1; k <= 6; k++) {
            assertEquals(1 / (k * Math.PI), result.roots().get(6 - k).root(), 1e-10);
        }
        assertTrue(result.suspectedDiscontinuities().isEmpty());
    }

    @Test
    void reportsSamplesThatHitARootExactly() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("x^2 - 1"), "x", -2, 2, 4,
                CRITERIA);
        assertEquals(2, result.identicallyZeroSamples());
        assertEquals(2, result.roots().size());
        assertEquals(-1.0, result.roots().get(0).root());
        assertEquals(1.0, result.roots().get(1).root());
    }

    @Test
    void flagsATouchingRootWithoutSignChangeAsPossibleDoubleRoot() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("(x - 1.01)^2"), "x", 0, 3, 100,
                CRITERIA);
        assertTrue(result.roots().isEmpty());
        assertEquals(1, result.possibleDoubleRoots().size());
        assertEquals(1.01, result.possibleDoubleRoots().get(0), 0.03);
    }

    @Test
    void simpleRootsAreNotFlaggedAsPossibleDoubleRoots() {
        RootScanner.ScanResult result = RootScanner.scan(ExpressionParser.parse("x^6 - x - 1"), "x", -2, 2, 400,
                CRITERIA);
        assertEquals(2, result.roots().size());
        assertTrue(result.possibleDoubleRoots().isEmpty());
    }

    @Test
    void invalidScanArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RootScanner.scan(ExpressionParser.parse("x"), "x", 1, 1, 10, CRITERIA));
        assertThrows(IllegalArgumentException.class,
                () -> RootScanner.scan(ExpressionParser.parse("x"), "x", 0, 1, 1, CRITERIA));
        assertThrows(IllegalArgumentException.class,
                () -> RootScanner.scan(ExpressionParser.parse("x"), "x", 0, Double.NaN, 10, CRITERIA));
    }
}
