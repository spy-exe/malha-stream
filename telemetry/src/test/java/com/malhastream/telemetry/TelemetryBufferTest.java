package com.malhastream.telemetry;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.StringWriter;

public class TelemetryBufferTest {
    @Test public void recordsInOrderAndBoundsMemory() throws Exception {
        TelemetryBuffer buffer = new TelemetryBuffer(2);
        buffer.record("queue_input", 11, 100, 0);
        buffer.record("output_available", 11, 200, 0);
        buffer.record("release_call", 11, 300, 1);
        TelemetryBuffer.Snapshot snapshot = buffer.snapshot();
        assertEquals(2, snapshot.size());
        assertEquals(1, snapshot.overwritten());
        StringWriter csv = new StringWriter();
        snapshot.writeCsv(csv);
        assertTrue(csv.toString().contains("output_available,11,200,0"));
        assertFalse(csv.toString().contains("queue_input,11,100,0"));
    }
    @Test public void snapshotDoesNotChangeAfterRecording() {
        TelemetryBuffer buffer = new TelemetryBuffer(3);
        buffer.record("input_dispatch", 1, 100, 2);
        TelemetryBuffer.Snapshot snapshot = buffer.snapshot();
        buffer.record("failure", 0, 101, -5);
        assertEquals(1, snapshot.size());
        assertEquals(2, buffer.snapshot().size());
    }
    @Test public void rejectsInvalidRecordsAndCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new TelemetryBuffer(0));
        TelemetryBuffer buffer = new TelemetryBuffer(2);
        assertThrows(IllegalArgumentException.class, () -> buffer.record("bad,event", 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> buffer.record("input", 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> buffer.record(null, 1, 0, 0));
    }
    @Test public void calculatesOnlyMatchedMonotonicIntervals() {
        TelemetryBuffer buffer = new TelemetryBuffer(20);
        buffer.record("queue_input", 3, 1_000_000, 0);
        buffer.record("output_available", 3, 4_000_000, 0);
        buffer.record("release_call", 3, 6_000_000, 1);
        buffer.record("output_available", 99, 9_000_000, 0); // no matching input
        buffer.record("queue_input", 4, 8_000_000, 0);
        buffer.record("output_available", 4, 7_000_000, 0); // invalid order
        assertArrayEquals(new double[]{3}, buffer.snapshot().intervalsMs("queue_input", "output_available"), 0);
        assertArrayEquals(new double[]{2}, buffer.snapshot().intervalsMs("output_available", "release_call"), 0);
        assertEquals(0, buffer.snapshot().intervalsMs("physical_input", "photon").length);
    }
    @Test public void consumesEachStartOnlyOnceAndIgnoresMissingFinish() {
        TelemetryBuffer buffer = new TelemetryBuffer(10);
        buffer.record("start", 1, 1, 0);
        buffer.record("end", 1, 2, 0);
        buffer.record("end", 1, 3, 0);
        buffer.record("start", 2, 4, 0);
        assertEquals(1, buffer.snapshot().intervalsMs("start", "end").length);
    }
}
