package numericalmethods.solver;

public record StoppingCriteria(double tolerance, int maxIterations) {

    public static final double DEFAULT_TOLERANCE = 1e-8;
    public static final int DEFAULT_MAX_ITERATIONS = 100;
    public static final int MAX_ALLOWED_ITERATIONS = 100_000;

    public StoppingCriteria {
        InputValidation.requirePositiveFinite("Tolerance", tolerance);
        InputValidation.requireInRange("Maximum iterations", maxIterations, 1, MAX_ALLOWED_ITERATIONS);
    }

    public static StoppingCriteria defaults() {
        return new StoppingCriteria(DEFAULT_TOLERANCE, DEFAULT_MAX_ITERATIONS);
    }

    public boolean isWithinTolerance(double difference, double reference) {
        return Math.abs(difference) <= tolerance * Math.max(1.0, Math.abs(reference));
    }
}
