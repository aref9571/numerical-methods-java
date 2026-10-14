package numericalmethods.solver;

public final class InputValidation {

    private InputValidation() {
    }

    public static double requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a finite number, but was " + value);
        }
        return value;
    }

    public static double requirePositiveFinite(String name, double value) {
        requireFinite(name, value);
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static int requireInRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum + ", but was " + value);
        }
        return value;
    }
}
