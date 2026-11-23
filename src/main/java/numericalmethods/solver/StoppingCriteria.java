package numericalmethods.solver;

public record StoppingCriteria(double tolerance, int maxIterations, ToleranceMode mode) {

    public static final double DEFAULT_TOLERANCE = 1e-8;
    public static final int DEFAULT_MAX_ITERATIONS = 100;
    public static final int MAX_ALLOWED_ITERATIONS = 100_000;

    public enum ToleranceMode {
        ABSOLUTE,
        RELATIVE
    }

    public StoppingCriteria {
        InputValidation.requirePositiveFinite("Tolerance", tolerance);
        InputValidation.requireInRange("Maximum iterations", maxIterations, 1, MAX_ALLOWED_ITERATIONS);
        if (mode == null) {
            throw new IllegalArgumentException("A tolerance mode is required");
        }
    }

    public StoppingCriteria(double tolerance, int maxIterations) {
        this(tolerance, maxIterations, ToleranceMode.RELATIVE);
    }

    public static StoppingCriteria defaults() {
        return new StoppingCriteria(DEFAULT_TOLERANCE, DEFAULT_MAX_ITERATIONS);
    }

    public static StoppingCriteria absolute(double tolerance, int maxIterations) {
        return new StoppingCriteria(tolerance, maxIterations, ToleranceMode.ABSOLUTE);
    }

    public boolean isWithinTolerance(double difference, double reference) {
        double scale = mode == ToleranceMode.ABSOLUTE ? 1.0 : Math.max(1.0, Math.abs(reference));
        return Math.abs(difference) <= tolerance * scale;
    }

    public double step(double reference) {
        return mode == ToleranceMode.ABSOLUTE ? tolerance : tolerance * Math.max(1.0, Math.abs(reference));
    }
}
