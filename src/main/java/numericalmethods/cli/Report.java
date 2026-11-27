package numericalmethods.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Report {

    public static final String INPUT = "Input";
    public static final String RESULT = "Result";
    public static final String DIAGNOSTICS = "Diagnostics";
    public static final String WARNINGS = "Warnings";

    private final String title;
    private final Map<String, List<String>> sections = new LinkedHashMap<>();

    public Report(String title) {
        this.title = title;
        sections.put(INPUT, new ArrayList<>());
        sections.put(RESULT, new ArrayList<>());
        sections.put(DIAGNOSTICS, new ArrayList<>());
    }

    public Report input(String line) {
        return add(INPUT, line);
    }

    public Report result(String line) {
        return add(RESULT, line);
    }

    public Report diagnostic(String line) {
        return add(DIAGNOSTICS, line);
    }

    public Report warning(String line) {
        return add(WARNINGS, line);
    }

    public Report table(String heading, List<String> lines) {
        sections.computeIfAbsent(heading, key -> new ArrayList<>()).addAll(lines);
        return this;
    }

    public Report add(String section, String line) {
        sections.computeIfAbsent(section, key -> new ArrayList<>()).add(line);
        return this;
    }

    public String title() {
        return title;
    }

    public List<String> sectionNames() {
        List<String> order = new ArrayList<>(sections.keySet());
        order.remove(WARNINGS);
        order.add(WARNINGS);
        order.removeIf(name -> sections.getOrDefault(name, List.of()).isEmpty());
        return order;
    }

    public List<String> lines(String section) {
        return List.copyOf(sections.getOrDefault(section, List.of()));
    }

    public String render() {
        StringBuilder text = new StringBuilder();
        text.append("== ").append(title).append(" ==").append(System.lineSeparator());
        List<String> order = new ArrayList<>(sections.keySet());
        order.remove(WARNINGS);
        order.add(WARNINGS);
        for (String name : order) {
            List<String> lines = sections.get(name);
            if (lines == null || lines.isEmpty()) {
                continue;
            }
            text.append(name).append(System.lineSeparator());
            for (String line : lines) {
                text.append("  ").append(line).append(System.lineSeparator());
            }
        }
        return text.toString();
    }
}
