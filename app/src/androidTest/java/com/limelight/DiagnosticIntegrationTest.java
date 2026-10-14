package com.limelight;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Runtime smoke/security checks. These do not benchmark a physical decoder. */
@RunWith(AndroidJUnit4.class)
public class DiagnosticIntegrationTest {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    @Test public void installsAlongsideOfficialMoonlightAndHasTvLauncher() {
        Context context = context();
        assertEquals("app.malhastream.debug", context.getPackageName());
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setPackage(context.getPackageName());
        assertNotNull(context.getPackageManager().resolveActivity(launcher, 0));
    }
    @Test public void exportProviderIsPrivateAndRejectsPathTraversalAndWrites() throws Exception {
        Context context = context();
        String authority = context.getPackageName() + ".benchmarks";
        ProviderInfo provider = context.getPackageManager().resolveContentProvider(authority, 0);
        assertNotNull(provider);
        assertFalse(provider.exported);
        for (String mode : new String[]{"r", "w"}) {
            try {
                context.getContentResolver().openFileDescriptor(Uri.parse("content://" + authority + "/../shared_prefs/test.xml"), mode);
                fail("Traversal or write unexpectedly accepted");
            } catch (java.io.FileNotFoundException expected) { }
        }
    }
}
