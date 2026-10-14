package numericalmethods.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InputValidationTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -3.5, 1e300, Double.MIN_VALUE})
    void finiteValuesAreReturnedUnchanged(double value) {
        assertEquals(value, InputValidation.requireFinite("x", value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteValuesAreRejectedWithTheirName(double value) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> InputValidation.requireFinite("Lower bound", value));

        assertEquals("Lower bound must be a finite number, but was " + value, exception.getMessage());
    }

    @Test
    void positiveFiniteAcceptsPositiveValues() {
        assertEquals(0.1, InputValidation.requirePositiveFinite("Step size", 0.1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0, -0.1})
    void positiveFiniteRejectsZeroAndNegativeValues(double value) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InputValidation.requirePositiveFinite("Step size", value));

        assertEquals("Step size must be greater than 0, but was " + value, exception.getMessage());
    }

    @Test
    void positiveFiniteRejectsNaN() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidation.requirePositiveFinite("Step size", Double.NaN));
    }

    @Test
    void rangeAcceptsBothEnds() {
        assertEquals(2, InputValidation.requireInRange("Subintervals", 2, 2, 10));
        assertEquals(10, InputValidation.requireInRange("Subintervals", 10, 2, 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 11})
    void rangeRejectsValuesOutside(int value) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InputValidation.requireInRange("Subintervals", value, 2, 10));

        assertEquals("Subintervals must be between 2 and 10, but was " + value, exception.getMessage());
    }
}
