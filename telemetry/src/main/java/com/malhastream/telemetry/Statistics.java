package com.malhastream.telemetry;
import java.util.Arrays;

public final class Statistics {
    private Statistics() {}
    /** Linear interpolation of order statistics; NaN means no data. */
    public static double percentile(double[] values, double quantile) {
        if (!Double.isFinite(quantile) || quantile < 0 || quantile > 1) {
            throw new IllegalArgumentException("Quantile must be between 0 and 1");
        }
        if (values.length == 0) return Double.NaN;
        double[] sorted = values.clone();
        for (double value : sorted) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Finite samples required");
        }
        Arrays.sort(sorted);
        double index = quantile * (sorted.length - 1);
        int lower = (int) index;
        int upper = (int) Math.ceil(index);
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower);
    }
}
