package numericalmethods.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StoppingCriteriaTest {

    @Test
    void defaultsUseDocumentedValues() {
        StoppingCriteria criteria = StoppingCriteria.defaults();

        assertEquals(1e-8, criteria.tolerance());
        assertEquals(100, criteria.maxIterations());
    }

    @Test
    void acceptsValidValuesAtTheBounds() {
        assertEquals(1, new StoppingCriteria(1e-12, 1).maxIterations());
        assertEquals(StoppingCriteria.MAX_ALLOWED_ITERATIONS,
                new StoppingCriteria(0.5, StoppingCriteria.MAX_ALLOWED_ITERATIONS).maxIterations());
        assertEquals(Double.MIN_VALUE, new StoppingCriteria(Double.MIN_VALUE, 10).tolerance());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0, -1e-8, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidTolerance(double tolerance) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new StoppingCriteria(tolerance, 100));

        assertTrue(exception.getMessage().startsWith("Tolerance must be"), exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, StoppingCriteria.MAX_ALLOWED_ITERATIONS + 1, Integer.MAX_VALUE})
    void rejectsInvalidIterationLimit(int maxIterations) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new StoppingCriteria(1e-8, maxIterations));

        assertTrue(exception.getMessage().startsWith("Maximum iterations must be between 1 and 100000"),
                exception.getMessage());
    }

    @Test
    void toleranceIsAbsoluteForSmallReferenceValues() {
        StoppingCriteria criteria = new StoppingCriteria(1e-6, 100);

        assertTrue(criteria.isWithinTolerance(1e-6, 0.0));
        assertTrue(criteria.isWithinTolerance(-5e-7, 0.001));
        assertFalse(criteria.isWithinTolerance(2e-6, 0.5));
    }

    @Test
    void toleranceIsRelativeForLargeReferenceValues() {
        StoppingCriteria criteria = new StoppingCriteria(1e-6, 100);

        assertTrue(criteria.isWithinTolerance(0.9, 1e6));
        assertTrue(criteria.isWithinTolerance(-0.9, -1e6));
        assertFalse(criteria.isWithinTolerance(2.0, 1e6));
    }

    @Test
    void nonFiniteDifferenceIsNeverWithinTolerance() {
        StoppingCriteria criteria = StoppingCriteria.defaults();

        assertFalse(criteria.isWithinTolerance(Double.NaN, 1.0));
        assertFalse(criteria.isWithinTolerance(Double.POSITIVE_INFINITY, 1.0));
    }
}
