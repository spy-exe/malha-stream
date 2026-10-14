package com.limelight.diagnostics;

import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public class ExportPreference extends Preference {
    public ExportPreference(Context context, AttributeSet attrs) { super(context, attrs); }
    @Override protected void onClick() {
        File directory = new File(getContext().getFilesDir(), "benchmarks");
        File[] sessions = directory.listFiles((dir, name) -> name.endsWith(".json"));
        File latest = null;
        if (sessions != null) for (File session : sessions) {
            if (latest == null || session.lastModified() > latest.lastModified()) latest = session;
        }
        if (latest == null) {
            Toast.makeText(getContext(), "Nenhuma sessão gravada. Ative o diagnóstico e encerre um stream.", Toast.LENGTH_LONG).show();
            return;
        }
        String base = latest.getName().substring(0, latest.getName().length() - 5);
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(Uri.parse("content://" + getContext().getPackageName() + ".benchmarks/" + base + ".json"));
        uris.add(Uri.parse("content://" + getContext().getPackageName() + ".benchmarks/" + base + ".csv"));
        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clip = ClipData.newRawUri("Benchmark", uris.get(0));
        clip.addItem(new ClipData.Item(uris.get(1)));
        share.setClipData(clip);
        try { getContext().startActivity(Intent.createChooser(share, "Exportar benchmark")); }
        catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(getContext(), "Nenhum app de compartilhamento disponível. Use a coleta por ADB.", Toast.LENGTH_LONG).show();
        }
    }
}
