package numericalmethods.roots;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public record IterativeRootResult(RootResult result, List<Double> iterates) {

    private static final double NOISE_LEVEL = 1e-13;

    public IterativeRootResult {
        Objects.requireNonNull(result, "result");
        iterates = List.copyOf(iterates);
        if (iterates.isEmpty()) {
            throw new IllegalArgumentException("At least the starting value must be recorded");
        }
    }

    public OptionalDouble observedOrder() {
        for (int k = iterates.size() - 1; k >= 3; k--) {
            double newest = Math.abs(iterates.get(k) - iterates.get(k - 1));
            double middle = Math.abs(iterates.get(k - 1) - iterates.get(k - 2));
            double oldest = Math.abs(iterates.get(k - 2) - iterates.get(k - 3));
            double scale = NOISE_LEVEL * Math.max(1.0, Math.abs(iterates.get(k)));
            if (newest <= scale || middle <= scale || oldest <= scale || middle >= oldest || newest >= middle) {
                continue;
            }
            double order = Math.log(newest / middle) / Math.log(middle / oldest);
            if (Double.isFinite(order)) {
                return OptionalDouble.of(order);
            }
        }
        return OptionalDouble.empty();
    }
}
