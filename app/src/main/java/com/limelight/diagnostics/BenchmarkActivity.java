package com.limelight.diagnostics;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import com.limelight.BuildConfig;

/** Debug-manifest-only ADB entry point. Never connects to or interrupts a host. */
public class BenchmarkActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!BuildConfig.MALHA_DIAGNOSTICS) { finish(); return; }
        String command = getIntent().getStringExtra("command");
        if ("export".equals(command)) {
            DiagnosticSession.exportActive(null);
        } else if ("configure".equals(command)) {
            String resolution = getIntent().getStringExtra("resolution");
            String codec = getIntent().getStringExtra("codec");
            String profile = getIntent().getStringExtra("profile");
            int fps = getIntent().getIntExtra("fps", 60);
            int bitrate = getIntent().getIntExtra("bitrate_kbps", 20000);
            if (("1920x1080".equals(resolution) || "1280x720".equals(resolution))
                    && ("h264".equals(codec) || "hevc".equals(codec)) && fps == 60
                    && bitrate >= 500 && bitrate <= 150000
                    && ("upstream".equals(profile) || "lowest-latency".equals(profile) || "balanced".equals(profile))) {
                SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("list_resolution", resolution).putString("list_fps", "60")
                        .putInt("seekbar_bitrate_kbps", bitrate)
                        .putString("video_format", "h264".equals(codec) ? "neverh265" : "forceh265")
                        .putString("malha_profile", profile).putBoolean("malha_diagnostics", true)
                        .putBoolean("checkbox_enable_hdr", false);
                if (!"upstream".equals(profile)) prefs.putString("frame_pacing", "balanced".equals(profile) ? "balanced" : "latency");
                prefs.apply();
            }
        }
        finish();
    }
}
