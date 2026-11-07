package numericalmethods.roots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;
import numericalmethods.expression.Expression;
import numericalmethods.expression.ExpressionParser;
import numericalmethods.solver.SolverStatus;
import numericalmethods.solver.StoppingCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class BracketingRootFinderTest {

    private static final double CUBIC_ROOT = 1.5213797068045676;

    enum Method {
        BISECTION,
        FALSE_POSITION;

        RootResult solve(String function, double lower, double upper, StoppingCriteria criteria) {
            return solve(function, "x", lower, upper, criteria);
        }

        RootResult solve(String function, String variable, double lower, double upper, StoppingCriteria criteria) {
            Expression expression = ExpressionParser.parse(function);
            return this == BISECTION
                    ? BracketingRootFinder.bisection(expression, variable, lower, upper, criteria)
                    : BracketingRootFinder.falsePosition(expression, variable, lower, upper, criteria);
        }

        RootResult solve(String function, double lower, double upper) {
            return solve(function, lower, upper, StoppingCriteria.defaults());
        }
    }

    private static void assertConvergedTo(double expectedRoot, RootResult result) {
        assertEquals(SolverStatus.CONVERGED, result.status(), result.message());
        assertTrue(Math.abs(result.root() - expectedRoot) <= result.errorEstimate(),
                "root " + result.root() + " is further than " + result.errorEstimate() + " from " + expectedRoot);
        assertTrue(result.errorEstimate() <= 1e-8 * Math.max(1, Math.abs(expectedRoot)),
                "error estimate " + result.errorEstimate() + " is above the tolerance");
    }

    private static double residual(String function, double x) {
        return ExpressionParser.parse(function).evaluate(Map.of("x", x));
    }

    static Stream<Arguments> knownRoots() {
        return Stream.of(
                Arguments.of("x^3 - x - 2", 1.0, 2.0, CUBIC_ROOT),
                Arguments.of("cos(x) - x", 0.0, 1.0, 0.7390851332151607),
                Arguments.of("sin(x)", 3.0, 4.0, Math.PI),
                Arguments.of("2 - x^2", 0.0, 2.0, Math.sqrt(2)),
                Arguments.of("exp(x) - 3", 0.0, 2.0, Math.log(3)),
                Arguments.of("x - 1e12", 0.0, 3e12, 1e12),
                Arguments.of("atan(x)", -1.0, 1e10, 0.0));
    }

    @ParameterizedTest
    @MethodSource("knownRoots")
    void bisectionFindsKnownRoots(String function, double lower, double upper, double expectedRoot) {
        RootResult result = Method.BISECTION.solve(function, lower, upper);

        assertConvergedTo(expectedRoot, result);
        assertEquals(residual(function, result.root()), result.functionValue());
    }

    @ParameterizedTest
    @MethodSource("knownRoots")
    void falsePositionFindsKnownRoots(String function, double lower, double upper, double expectedRoot) {
        RootResult result = Method.FALSE_POSITION.solve(function, lower, upper);

        assertConvergedTo(expectedRoot, result);
        assertEquals(residual(function, result.root()), result.functionValue());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void cubicResidualIsSmall(Method method) {
        RootResult result = method.solve("x^3 - x - 2", 1.0, 2.0);

        assertTrue(Math.abs(result.functionValue()) < 1e-6, "residual " + result.functionValue());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void otherVariableNamesWork(Method method) {
        RootResult result = method.solve("t^2 - 2", "t", 0.0, 2.0, StoppingCriteria.defaults());

        assertConvergedTo(Math.sqrt(2), result);
    }

    @Test
    void bisectionHalvesTheIntervalUntilItIsWithinTheTolerance() {
        RootResult result = Method.BISECTION.solve("x^3 - x - 2", 1.0, 2.0, new StoppingCriteria(1e-6, 100));

        assertEquals(20, result.iterations());
        assertEquals(Math.scalb(1.0, -20), result.errorEstimate());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void smallerToleranceNeedsMoreIterationsAndGivesASmallerEstimate(Method method) {
        RootResult coarse = method.solve("x^3 - x - 2", 1.0, 2.0, new StoppingCriteria(1e-4, 100));
        RootResult fine = method.solve("x^3 - x - 2", 1.0, 2.0, new StoppingCriteria(1e-12, 100));

        assertTrue(coarse.hasConverged() && fine.hasConverged());
        assertTrue(fine.iterations() > coarse.iterations());
        assertTrue(fine.errorEstimate() < coarse.errorEstimate());
        assertTrue(Math.abs(fine.root() - CUBIC_ROOT) <= fine.errorEstimate());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void rootAtLowerEndpointIsReturnedWithoutIterating(Method method) {
        RootResult result = method.solve("x^2 - 4", 2.0, 5.0);

        assertEquals(RootResult.converged(2.0, 0.0, 0.0, 0), result);
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void rootAtUpperEndpointIsReturnedWithoutIterating(Method method) {
        RootResult result = method.solve("x^2 - 4", -1.0, 2.0);

        assertEquals(RootResult.converged(2.0, 0.0, 0.0, 0), result);
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void rootsAtBothEndpointsReturnTheLowerOne(Method method) {
        RootResult result = method.solve("x^2 - 1", -1.0, 1.0);

        assertEquals(RootResult.converged(-1.0, 0.0, 0.0, 0), result);
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void exactRootFoundDuringIterationStopsImmediately(Method method) {
        RootResult result = method.solve("x - 1", 0.0, 2.0);

        assertEquals(RootResult.converged(1.0, 0.0, 0.0, 1), result);
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void smallestPossibleIntervalAroundARootConverges(Method method) {
        double lower = Math.nextDown(CUBIC_ROOT);
        double upper = Math.nextUp(CUBIC_ROOT);

        RootResult result = method.solve("x^3 - x - 2", lower, upper);

        assertTrue(result.hasConverged(), result.message());
        assertTrue(result.root() >= lower && result.root() <= upper);
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void reversedOrEmptyIntervalsAreRejected(Method method) {
        IllegalArgumentException reversed = assertThrows(IllegalArgumentException.class,
                () -> method.solve("x^3 - x - 2", 2.0, 1.0));
        assertEquals("The lower endpoint must be less than the upper endpoint, but the interval was [2.0, 1.0]",
                reversed.getMessage());

        assertThrows(IllegalArgumentException.class, () -> method.solve("x - 1", 1.0, 1.0));
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void nonFiniteEndpointsAreRejected(Method method) {
        assertThrows(IllegalArgumentException.class, () -> method.solve("x", Double.NaN, 1.0));
        assertThrows(IllegalArgumentException.class, () -> method.solve("x", -1.0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> method.solve("x", Double.NEGATIVE_INFINITY, 1.0));
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void intervalWiderThanTheLargestDoubleIsRejected(Method method) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> method.solve("x", -Double.MAX_VALUE, Double.MAX_VALUE));

        assertTrue(exception.getMessage().contains("too wide"), exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void intervalWithoutSignChangeIsRejected(Method method) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> method.solve("x^2 + 1", -1.0, 2.0));

        assertEquals("The function has the same sign at both endpoints (f(-1.0) = 2.0, f(2.0) = 5.0),"
                + " so the interval is not known to contain a root", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void doubleRootWithoutSignChangeCannotBeBracketed(Method method) {
        assertThrows(IllegalArgumentException.class, () -> method.solve("(x - 1)^2", 0.0, 3.0));
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void undefinedEndpointIsRejected(Method method) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> method.solve("ln(x)", 0.0, 2.0));

        assertTrue(exception.getMessage().startsWith("The function is undefined at the endpoint x = 0.0"),
                exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void functionOfAnotherVariableIsRejected(Method method) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> method.solve("y - 1", 0.0, 2.0));

        assertTrue(exception.getMessage().contains("No value given for variable 'y'"), exception.getMessage());
    }

    @Test
    void nullArgumentsAreRejected() {
        Expression function = ExpressionParser.parse("x - 1");
        StoppingCriteria criteria = StoppingCriteria.defaults();

        assertThrows(NullPointerException.class, () -> BracketingRootFinder.bisection(null, "x", 0, 2, criteria));
        assertThrows(NullPointerException.class, () -> BracketingRootFinder.bisection(function, null, 0, 2, criteria));
        assertThrows(NullPointerException.class, () -> BracketingRootFinder.falsePosition(function, "x", 0, 2, null));
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void poleIsNotReportedAsARoot(Method method) {
        RootResult result = method.solve("tan(x)", 1.0, 2.0);

        assertEquals(SolverStatus.BREAKDOWN, result.status());
        assertTrue(result.message().contains("probably discontinuous"), result.message());
        assertEquals(Math.PI / 2, result.root(), 1e-6);
    }

    @Test
    void bisectionReportsAPoleWhereTheFunctionGrows() {
        RootResult result = Method.BISECTION.solve("1/x", -1.0, 2.0);

        assertFalse(result.hasConverged());
        assertTrue(result.message().contains("probably discontinuous"), result.message());
    }

    @Test
    void bisectionReportsAnUndefinedMidpoint() {
        RootResult result = Method.BISECTION.solve("1/x", -1.0, 1.0);

        assertEquals(SolverStatus.BREAKDOWN, result.status());
        assertEquals("The function is undefined at x = 0.0 (Division by zero)", result.message());
        assertEquals(0, result.iterations());
        assertEquals(-1.0, result.root());
        assertEquals(2.0, result.errorEstimate());
    }

    @Test
    void falsePositionReportsAnUndefinedPoint() {
        RootResult result = Method.FALSE_POSITION.solve("1/x", -1.0, 2.0);

        assertEquals(SolverStatus.BREAKDOWN, result.status());
        assertEquals("The function is undefined at x = 0.0 (Division by zero)", result.message());
        assertEquals(1, result.iterations());
        assertEquals(1.0, result.root());
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void iterationLimitIsReportedWithTheLastApproximation(Method method) {
        RootResult result = method.solve("x^3 - x - 2", 1.0, 2.0, new StoppingCriteria(1e-8, 5));

        assertEquals(SolverStatus.ITERATION_LIMIT_REACHED, result.status());
        assertEquals(5, result.iterations());
        assertTrue(Math.abs(result.root() - CUBIC_ROOT) <= result.errorEstimate());
        assertEquals(residual("x^3 - x - 2", result.root()), result.functionValue());
    }

    @Test
    void falsePositionIsSlowWhenOneEndpointNeverMoves() {
        RootResult bisection = Method.BISECTION.solve("x^10 - 1", 0.0, 1.3);
        RootResult falsePosition = Method.FALSE_POSITION.solve("x^10 - 1", 0.0, 1.3);

        assertConvergedTo(1.0, bisection);
        assertConvergedTo(1.0, falsePosition);
        assertTrue(falsePosition.iterations() > 2 * bisection.iterations(),
                falsePosition.iterations() + " vs " + bisection.iterations());
    }

    @Test
    void falsePositionDoesNotClaimConvergenceWhenItStalls() {
        RootResult bisection = Method.BISECTION.solve("(x - 1)^3", 0.0, 3.0);
        RootResult falsePosition = Method.FALSE_POSITION.solve("(x - 1)^3", 0.0, 3.0);

        assertConvergedTo(1.0, bisection);
        assertEquals(SolverStatus.ITERATION_LIMIT_REACHED, falsePosition.status());
        assertTrue(Math.abs(falsePosition.root() - 1.0) <= falsePosition.errorEstimate());
    }

    @Test
    void bisectionReportsWhenTheToleranceIsBelowDoublePrecision() {
        RootResult result = Method.BISECTION.solve("x^2 - 2", 1.0, 2.0, new StoppingCriteria(1e-20, 1000));

        assertEquals(SolverStatus.BREAKDOWN, result.status());
        assertTrue(result.message().contains("cannot be split further"), result.message());
        assertEquals(Math.sqrt(2), result.root(), 1e-15);
        assertTrue(result.iterations() < 60);
    }
}
