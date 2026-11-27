package numericalmethods.text;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

public final class NumberText {

    public static final int DEFAULT_SIGNIFICANT_DIGITS = 10;

    private static final double SMALLEST_PLAIN = 1e-5;
    private static final double LARGEST_PLAIN = 1e10;

    private NumberText() {
    }

    public static String format(double value) {
        return format(value, DEFAULT_SIGNIFICANT_DIGITS);
    }

    public static String format(double value, int significantDigits) {
        if (Double.isNaN(value)) {
            return "undefined";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "infinity" : "-infinity";
        }
        if (value == 0) {
            return "0";
        }
        double magnitude = Math.abs(value);
        if (magnitude >= SMALLEST_PLAIN && magnitude < LARGEST_PLAIN) {
            BigDecimal rounded = new BigDecimal(value).round(new MathContext(significantDigits)).stripTrailingZeros();
            return rounded.toPlainString();
        }
        String scientific = String.format(Locale.ROOT, "%." + (significantDigits - 1) + "e", value);
        int exponentStart = scientific.indexOf('e');
        String mantissa = trimZeros(scientific.substring(0, exponentStart));
        int exponent = Integer.parseInt(scientific.substring(exponentStart + 1));
        return mantissa + "e" + exponent;
    }

    private static String trimZeros(String mantissa) {
        if (!mantissa.contains(".")) {
            return mantissa;
        }
        String trimmed = mantissa.replaceAll("0+$", "");
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
