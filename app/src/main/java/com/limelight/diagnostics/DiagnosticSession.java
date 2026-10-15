package com.limelight.diagnostics;

import android.app.Activity;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.preference.PreferenceManager;
import com.limelight.BuildConfig;
import com.limelight.LimeLog;
import com.limelight.preferences.PreferenceConfiguration;
import com.malhastream.telemetry.Statistics;
import com.malhastream.telemetry.TelemetryBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Informational instrumentation only; never changes codec or presentation policy. */
public final class DiagnosticSession {
    private static volatile DiagnosticSession current;
    private final Context context;
    private final Activity activity;
    private final TelemetryBuffer events = new TelemetryBuffer(131072);
    private final long[] outputPts = new long[256];
    private final JSONObject metadata = new JSONObject();
    private final String sessionId = UUID.randomUUID().toString();
    private final long startNs = System.nanoTime();
    private final HandlerThread observer = new HandlerThread("Malha diagnostics");
    private final Handler handler;
    private final AtomicBoolean exporting = new AtomicBoolean();
    private final ExecutorService exporter = Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private int generation;
    private final boolean traceEnabled;

    public static DiagnosticSession begin(Activity activity, PreferenceConfiguration prefs) {
        if (!BuildConfig.MALHA_DIAGNOSTICS || !PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean("malha_diagnostics", false)) return null;
        try { return new DiagnosticSession(activity, prefs); }
        catch (Exception error) {
            LimeLog.warning("Malha diagnostics unavailable: " + error.getClass().getSimpleName());
            return null;
        }
    }

    private DiagnosticSession(Activity activity, PreferenceConfiguration prefs) throws Exception {
        this.activity = activity;
        context = activity.getApplicationContext();
        traceEnabled = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("malha_trace", false);
        Arrays.fill(outputPts, -1);
        metadata.put("schema_version", 1).put("session_id", sessionId)
                .put("commit", BuildConfig.MALHA_COMMIT).put("apk_version", BuildConfig.VERSION_NAME)
                .put("application_id", BuildConfig.APPLICATION_ID).put("clock", "System.nanoTime; local device only")
                .put("start_monotonic_ns", startNs).put("input_to_photon_ms", JSONObject.NULL)
                .put("physical_validation", "BLOCKED: requires external measurement")
                .put("width", prefs.width).put("height", prefs.height).put("fps", prefs.fps)
                .put("bitrate_kbps", prefs.bitrate).put("hdr", prefs.enableHdr)
                .put("frame_pacing", prefs.framePacing).put("requested_codec", prefs.videoFormat.name())
                .put("profile", PreferenceManager.getDefaultSharedPreferences(activity).getString("malha_profile", "upstream"))
                .put("amlogic_experiment_requested", PreferenceManager.getDefaultSharedPreferences(activity)
                        .getBoolean("malha_amlogic_hevc_experiment", false))
                .put("model", Build.MODEL).put("manufacturer", Build.MANUFACTURER)
                .put("device", Build.DEVICE).put("hardware", Build.HARDWARE).put("board", Build.BOARD)
                .put("android_api", Build.VERSION.SDK_INT).put("android_release", Build.VERSION.RELEASE)
                .put("display_hz_at_start", activity.getWindowManager().getDefaultDisplay().getRefreshRate())
                .put("display_hz_origin", "Android Display.getRefreshRate; driver-reported, not measured externally")
                .put("measured_display_hz", JSONObject.NULL)
                .put("visible_fps", JSONObject.NULL).put("chip_temperature_c", JSONObject.NULL)
                .put("surface_render_note", "Callback is informational, may be delayed/batched/missing; does not prove visible photons")
                .put("native_receive_note", "Native client assembly duration only; not one-way network latency")
                .put("host_processing_note", "Host-reported duration in tenths of ms; zero means unavailable")
                .put("instrumentation_note", "Opt-in observer; bounded ring and periodic resource samples may add overhead. Compare with recording disabled.");
        metadata.put("android_trace_enabled", traceEnabled);
        if (Build.VERSION.SDK_INT >= 31) {
            metadata.put("soc_model", Build.SOC_MODEL).put("soc_manufacturer", Build.SOC_MANUFACTURER);
        }
        metadata.put("codecs", codecInventory());
        observer.start();
        handler = new Handler(observer.getLooper());
        current = this;
        handler.post(new Runnable() {
            @Override public void run() {
                if (closed) return;
                try {
                    record("process_cpu_ms", 0, Process.getElapsedCpuTime());
                    Debug.MemoryInfo memory = new Debug.MemoryInfo();
                    Debug.getMemoryInfo(memory);
                    record("process_pss_kb", 0, memory.getTotalPss());
                    if (Build.VERSION.SDK_INT >= 29) {
                        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        if (power != null) record("thermal_status", 0, power.getCurrentThermalStatus());
                    }
                } catch (RuntimeException unavailable) { record("resource_sample_unavailable", 0, 0); }
                handler.postDelayed(this, 1000);
            }
        });
    }

    private static JSONArray codecInventory() throws Exception {
        JSONArray result = new JSONArray();
        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()) {
            if (info.isEncoder()) continue;
            for (String mime : info.getSupportedTypes()) {
                if (!mime.equals("video/avc") && !mime.equals("video/hevc") && !mime.equals("video/av01")) continue;
                JSONObject entry = new JSONObject().put("name", info.getName()).put("mime", mime);
                if (Build.VERSION.SDK_INT >= 29) entry.put("hardware_accelerated", info.isHardwareAccelerated());
                else entry.put("hardware_accelerated", JSONObject.NULL);
                if (Build.VERSION.SDK_INT >= 30) {
                    try { entry.put("declares_low_latency", info.getCapabilitiesForType(mime)
                            .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)); }
                    catch (RuntimeException error) { entry.put("declares_low_latency", JSONObject.NULL); }
                } else entry.put("declares_low_latency", JSONObject.NULL);
                result.put(entry);
            }
        }
        return result;
    }

    public void record(String name, long id, long value) {
        if (!closed) {
            events.record(name, id, System.nanoTime(), value);
            if (traceEnabled) {
                if (Build.VERSION.SDK_INT >= 29) android.os.Trace.setCounter(name, id);
                else { android.os.Trace.beginSection(name); android.os.Trace.endSection(); }
            }
        }
    }
    public void recordAt(String name, long id, long ns, long value) {
        if (!closed && ns >= 0) events.record(name, id, ns, value);
    }
    public void configured(MediaCodec codec, MediaFormat format) {
        synchronized (this) {
            Arrays.fill(outputPts, -1);
            generation++;
            try {
                metadata.put("selected_decoder", codec.getName()).put("codec", format.getString(MediaFormat.KEY_MIME))
                        .put("configured_format", format.toString()).put("codec_generation", generation)
                        .put("configured_hdr_transfer", format.containsKey("color-transfer")
                                ? format.getInteger("color-transfer") : JSONObject.NULL)
                        .put("display_hz_after_config", activity.getWindowManager().getDefaultDisplay().getRefreshRate());
            } catch (Exception ignored) { record("metadata_failure", 0, generation); }
        }
        record("codec_configured", 0, generation);
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                codec.setOnFrameRenderedListener((instance, pts, ns) -> {
                    recordAt("surface_render_callback", pts, ns, 0);
                    record("surface_callback_delivery", pts, 0);
                }, handler);
            } catch (RuntimeException error) { record("render_callback_unavailable", 0, generation); }
        }
    }
    public synchronized void output(int index, long pts) {
        if (index >= 0 && index < outputPts.length) outputPts[index] = pts;
        else record("untracked_output_index", index, 0);
        record("output_available", pts, 0);
    }
    public synchronized long release(int index, boolean render, long requestedNs) {
        long pts = index >= 0 && index < outputPts.length ? outputPts[index] : -1;
        if (index >= 0 && index < outputPts.length) outputPts[index] = -1;
        if (pts >= 0) {
            record(render ? "release_call" : "output_drop", pts, requestedNs);
        }
        return pts;
    }
    public static void input(String type, int code, long eventTimeMs) {
        DiagnosticSession active = current;
        if (active != null) {
            active.record(type, code, android.os.SystemClock.uptimeMillis() - eventTimeMs);
        }
    }
    public static void controllerForwarded(short controller, int buttons) {
        DiagnosticSession active = current;
        if (active != null) active.record("controller_forward", controller, buttons & 0xffffffffL);
    }
    public static boolean exportActive(Runnable completion) {
        DiagnosticSession active = current;
        if (active == null) return false;
        return active.export(completion);
    }
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (current == this) current = null;
        handler.removeCallbacksAndMessages(null);
        observer.quitSafely();
        enqueueExport(null);
        exporter.shutdown();
    }
    private synchronized boolean export(Runnable completion) {
        if (closed || !exporting.compareAndSet(false, true)) return false;
        enqueueExport(completion);
        return true;
    }
    private void enqueueExport(Runnable completion) {
        TelemetryBuffer.Snapshot snapshot = events.snapshot();
        long capturedNs = System.nanoTime();
        boolean closedAtCapture = closed;
        exporter.execute(() -> {
            try {
                JSONObject output;
                synchronized (this) { output = new JSONObject(metadata.toString()); }
                output.put("capture_monotonic_ns", capturedNs).put("duration_s", (capturedNs - startNs) / 1_000_000_000.0)
                        .put("event_count", snapshot.size()).put("overwritten_events", snapshot.overwritten())
                        .put("session_closed", closedAtCapture);
                JSONObject metrics = new JSONObject();
                metric(metrics, snapshot, "feed_work_ms", "submit_picture", "queue_input");
                metric(metrics, snapshot, "queue_input_call_ms", "queue_input", "queue_input_return");
                metric(metrics, snapshot, "queue_to_output_ms", "queue_input", "output_available");
                metric(metrics, snapshot, "output_to_release_ms", "output_available", "release_call");
                metric(metrics, snapshot, "release_to_surface_callback_ms", "release_call", "surface_render_callback");
                output.put("metrics", metrics);
                File directory = new File(context.getFilesDir(), "benchmarks");
                if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException("Export directory unavailable");
                File csv = new File(directory, sessionId + ".csv");
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(csv), StandardCharsets.UTF_8)) {
                    snapshot.writeCsv(writer);
                }
                File json = new File(directory, sessionId + ".json");
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(json), StandardCharsets.UTF_8)) {
                    writer.write(output.toString(2));
                }
                if (completion != null) handlerOnMain(completion);
            } catch (Exception error) {
                LimeLog.warning("Malha export failed: " + error.getClass().getSimpleName());
            } finally { exporting.set(false); }
        });
    }
    private static void metric(JSONObject output, TelemetryBuffer.Snapshot snapshot, String name,
                               String start, String end) throws Exception {
        double[] values = snapshot.intervalsMs(start, end);
        JSONObject metric = new JSONObject().put("n", values.length).put("origin", "direct local monotonic interval");
        for (double q : new double[]{.5, .9, .95, .99}) {
            double value = Statistics.percentile(values, q);
            boolean enough = (q < .95 || values.length >= 100) && (q < .99 || values.length >= 1000);
            metric.put("p" + (int)(q * 100), Double.isNaN(value) || !enough ? JSONObject.NULL : value);
        }
        metric.put("tail_policy", "p95 n>=100; p99 n>=1000");
        output.put(name, metric);
    }
    private void handlerOnMain(Runnable runnable) { new Handler(context.getMainLooper()).post(runnable); }
}
