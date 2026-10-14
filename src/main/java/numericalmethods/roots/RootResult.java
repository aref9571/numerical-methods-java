package numericalmethods.roots;

import java.util.Objects;
import numericalmethods.solver.InputValidation;
import numericalmethods.solver.SolverStatus;

public record RootResult(
        double root,
        double functionValue,
        double errorEstimate,
        int iterations,
        SolverStatus status,
        String message) {

    public RootResult {
        InputValidation.requireFinite("Root", root);
        InputValidation.requireFinite("Function value", functionValue);
        InputValidation.requireFinite("Error estimate", errorEstimate);
        if (errorEstimate < 0) {
            throw new IllegalArgumentException("Error estimate must not be negative, but was " + errorEstimate);
        }
        if (iterations < 0) {
            throw new IllegalArgumentException("Iterations must not be negative, but was " + iterations);
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
    }

    public static RootResult converged(double root, double functionValue, double errorEstimate, int iterations) {
        return new RootResult(root, functionValue, errorEstimate, iterations, SolverStatus.CONVERGED,
                "Converged after " + iterations + " iterations");
    }

    public static RootResult iterationLimitReached(
            double root, double functionValue, double errorEstimate, int iterations) {
        return new RootResult(root, functionValue, errorEstimate, iterations, SolverStatus.ITERATION_LIMIT_REACHED,
                "Did not converge within " + iterations + " iterations");
    }

    public static RootResult breakdown(
            double root, double functionValue, double errorEstimate, int iterations, String reason) {
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A breakdown needs a reason");
        }
        return new RootResult(root, functionValue, errorEstimate, iterations, SolverStatus.BREAKDOWN, reason);
    }

    public boolean hasConverged() {
        return status == SolverStatus.CONVERGED;
    }
}
