package numericalmethods.cli;

import java.util.ArrayList;
import java.util.List;

public final class TextTable {

    private final List<String[]> rows = new ArrayList<>();
    private final int columns;

    public TextTable(String... headers) {
        this.columns = headers.length;
        rows.add(headers.clone());
    }

    public TextTable row(String... cells) {
        if (cells.length != columns) {
            throw new IllegalArgumentException("Expected " + columns + " cells but got " + cells.length);
        }
        rows.add(cells.clone());
        return this;
    }

    public List<String> lines() {
        int[] widths = new int[columns];
        for (String[] row : rows) {
            for (int c = 0; c < columns; c++) {
                widths[c] = Math.max(widths[c], row[c].length());
            }
        }
        List<String> lines = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            lines.add(format(rows.get(r), widths));
            if (r == 0) {
                StringBuilder rule = new StringBuilder();
                for (int c = 0; c < columns; c++) {
                    if (c > 0) {
                        rule.append("-+-");
                    }
                    rule.append("-".repeat(widths[c]));
                }
                lines.add(rule.toString());
            }
        }
        return lines;
    }

    private static String format(String[] row, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < row.length; c++) {
            if (c > 0) {
                line.append(" | ");
            }
            line.append(row[c]).append(" ".repeat(widths[c] - row[c].length()));
        }
        return line.toString().stripTrailing();
    }
}
