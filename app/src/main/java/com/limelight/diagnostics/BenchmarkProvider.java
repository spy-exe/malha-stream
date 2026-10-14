package com.limelight.diagnostics;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Grants access only to explicitly shared session exports. */
public class BenchmarkProvider extends ContentProvider {
    private File file(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[a-f0-9-]{36}\\.(json|csv)")) throw new FileNotFoundException();
        return new File(new File(getContext().getFilesDir(), "benchmarks"), name);
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return uri.toString().endsWith(".csv") ? "text/csv" : "application/json"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        try { File file = file(uri); cursor.addRow(new Object[]{file.getName(), file.length()}); }
        catch (FileNotFoundException ignored) { }
        return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
