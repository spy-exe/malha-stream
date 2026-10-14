package com.malhastream.telemetry;

/** Experimental workaround, never a global Amlogic default. */
public final class DecoderExperimentPolicy {
    private DecoderExperimentPolicy() {}
    public static boolean skipLowLatency(String model, int api, String decoder, String mime, boolean optedIn) {
        return optedIn && api == 34 && "MiTV-AYFR0".equalsIgnoreCase(model)
                && "video/hevc".equals(mime)
                && "c2.amlogic.hevc.decoder".equalsIgnoreCase(decoder);
    }
}
