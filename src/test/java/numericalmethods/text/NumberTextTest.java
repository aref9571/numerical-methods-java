package numericalmethods.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NumberTextTest {

    @ParameterizedTest
    @CsvSource({
        "0.0, 0",
        "-0.0, 0",
        "2.0, 2",
        "1.1337890625, 1.133789063",
        "-1234.5, -1234.5",
        "0.00001, 0.00001",
        "0.000001, 1e-6",
        "-8.881784197001252E-16, -8.881784197e-16",
        "1.0E10, 1e10",
        "123456789.123, 123456789.1"
    })
    void formatsWithTenSignificantDigits(double value, String expected) {
        assertEquals(expected, NumberText.format(value));
    }

    @ParameterizedTest
    @CsvSource({"1.6180339887, 3, 1.62", "2.0, 3, 2", "0.000123456, 2, 0.00012", "0.00000123456, 2, 1.2e-6"})
    void formatsWithAChosenNumberOfDigits(double value, int digits, String expected) {
        assertEquals(expected, NumberText.format(value, digits));
    }

    @ParameterizedTest
    @CsvSource({"NaN, undefined", "Infinity, infinity", "-Infinity, -infinity"})
    void namesNonFiniteValues(double value, String expected) {
        assertEquals(expected, NumberText.format(value));
    }
}
