package com.malhastream.telemetry;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Bounded diagnostic events. No file I/O or per-event object allocation. */
public final class TelemetryBuffer {
    private final String[] event;
    private final long[] id, ns, value;
    private int next, count;
    private long overwritten;

    public TelemetryBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Positive capacity required");
        event = new String[capacity];
        id = new long[capacity];
        ns = new long[capacity];
        value = new long[capacity];
    }

    public synchronized void record(String name, long identifier, long timestampNs, long data) {
        if (name == null || name.indexOf(',') >= 0 || name.indexOf('\n') >= 0 || timestampNs < 0) {
            throw new IllegalArgumentException("Invalid event");
        }
        event[next] = name;
        id[next] = identifier;
        ns[next] = timestampNs;
        value[next] = data;
        next = (next + 1) % event.length;
        if (count < event.length) count++;
        else overwritten++;
    }

    public synchronized Snapshot snapshot() {
        String[] names = new String[count];
        long[] ids = new long[count], times = new long[count], values = new long[count];
        int start = (next - count + event.length) % event.length;
        for (int i = 0; i < count; i++) {
            int index = (start + i) % event.length;
            names[i] = event[index];
            ids[i] = id[index];
            times[i] = ns[index];
            values[i] = value[index];
        }
        return new Snapshot(names, ids, times, values, overwritten);
    }

    public static final class Snapshot {
        private final String[] event;
        private final long[] id, ns, value;
        private final long overwritten;
        private Snapshot(String[] event, long[] id, long[] ns, long[] value, long overwritten) {
            this.event = event; this.id = id; this.ns = ns; this.value = value;
            this.overwritten = overwritten;
        }
        public int size() { return event.length; }
        public long overwritten() { return overwritten; }
        public void writeCsv(Writer writer) throws IOException {
            writer.write("event,id,monotonic_ns,value\n");
            for (int i = 0; i < size(); i++) {
                writer.write(event[i] + "," + id[i] + "," + ns[i] + "," + value[i] + "\n");
            }
        }
        /** Only pairs same-ID events recorded on the local monotonic clock. */
        public double[] intervalsMs(String startEvent, String endEvent) {
            Map<Long, Long> starts = new HashMap<>();
            ArrayList<Double> result = new ArrayList<>();
            for (int i = 0; i < size(); i++) {
                if (event[i].equals(startEvent)) starts.put(id[i], ns[i]);
                else if (event[i].equals(endEvent)) {
                    Long start = starts.remove(id[i]);
                    if (start != null && ns[i] >= start) result.add((ns[i] - start) / 1_000_000.0);
                }
            }
            double[] values = new double[result.size()];
            for (int i = 0; i < values.length; i++) values[i] = result.get(i);
            return values;
        }
    }
}
