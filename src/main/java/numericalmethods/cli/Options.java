package numericalmethods.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Options {

    private final Map<String, String> values;

    private Options(Map<String, String> values) {
        this.values = values;
    }

    public static Options parse(List<String> parts, Set<String> allowed) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String[] keyAndValue = part.trim().split("\\s+", 2);
            String key = keyAndValue[0].toLowerCase();
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unknown option '" + key + "'; allowed options: " + allowed);
            }
            if (keyAndValue.length < 2 || keyAndValue[1].isBlank()) {
                throw new IllegalArgumentException("Option '" + key + "' needs a value, for example '" + key + " 10'");
            }
            if (values.put(key, keyAndValue[1].trim()) != null) {
                throw new IllegalArgumentException("Option '" + key + "' is given twice");
            }
        }
        return new Options(values);
    }

    public Optional<String> text(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public double number(String key, double fallback) {
        return text(key).map(Inputs::number).orElse(fallback);
    }

    public int integer(String key, int fallback) {
        return text(key).map(Inputs::integer).orElse(fallback);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }
}
