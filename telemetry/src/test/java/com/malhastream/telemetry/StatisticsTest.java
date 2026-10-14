package com.malhastream.telemetry;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticsTest {
    @Test public void interpolatesWithoutMutatingSamples() {
        double[] values = {9, 1, 5};
        assertEquals(5, Statistics.percentile(values, .5), 0);
        assertEquals(8.6, Statistics.percentile(values, .95), .000001);
        assertArrayEquals(new double[]{9,1,5}, values, 0);
        assertEquals(1, Statistics.percentile(values, 0), 0);
        assertEquals(9, Statistics.percentile(values, 1), 0);
    }
    @Test public void representsUnavailableDataExplicitly() {
        assertTrue(Double.isNaN(Statistics.percentile(new double[0], .5)));
        assertEquals(2, Statistics.percentile(new double[]{2}, .99), 0);
    }
    @Test public void rejectsInvalidStatistics() {
        assertThrows(IllegalArgumentException.class, () -> Statistics.percentile(new double[]{1}, -1));
        assertThrows(IllegalArgumentException.class, () -> Statistics.percentile(new double[]{1}, 2));
        assertThrows(IllegalArgumentException.class, () -> Statistics.percentile(new double[]{Double.NaN}, .5));
        assertThrows(IllegalArgumentException.class, () -> Statistics.percentile(new double[]{Double.POSITIVE_INFINITY}, .5));
        assertThrows(IllegalArgumentException.class, () -> Statistics.percentile(new double[]{1}, Double.NaN));
    }
}
