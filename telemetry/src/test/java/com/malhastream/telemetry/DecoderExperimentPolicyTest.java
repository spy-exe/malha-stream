package com.malhastream.telemetry;
import org.junit.Test;
import static org.junit.Assert.*;

public class DecoderExperimentPolicyTest {
    @Test public void requiresExplicitOptIn() {
        assertFalse(DecoderExperimentPolicy.skipLowLatency("MiTV-AYFR0", 34, "c2.amlogic.hevc.decoder", "video/hevc", false));
        assertTrue(DecoderExperimentPolicy.skipLowLatency("MiTV-AYFR0", 34, "c2.amlogic.hevc.decoder", "video/hevc", true));
    }
    @Test public void neverBroadlyDisablesAmlogicOrFireTvRequiredFlags() {
        for (String model : new String[]{"AFTM","AFTN","AFTMM","jaws","unknown",null}) {
            assertFalse(DecoderExperimentPolicy.skipLowLatency(model,34,"c2.amlogic.hevc.decoder","video/hevc",true));
        }
        assertFalse(DecoderExperimentPolicy.skipLowLatency("MiTV-AYFR0",34,"OMX.amlogic.hevc.decoder.awesome2","video/hevc",true));
    }
    @Test public void rejectsOtherCodecsAndFirmwareGenerations() {
        for (String mime : new String[]{"video/avc","video/av01","video/dolby-vision",null}) {
            assertFalse(DecoderExperimentPolicy.skipLowLatency("MiTV-AYFR0",34,"c2.amlogic.hevc.decoder",mime,true));
        }
        for (int api : new int[]{21,28,33,35,36}) {
            assertFalse(DecoderExperimentPolicy.skipLowLatency("MiTV-AYFR0",api,"c2.amlogic.hevc.decoder","video/hevc",true));
        }
    }
    @Test public void rejectsUnknownDecoderVariantsAndVendors() {
        for (String decoder : new String[]{"c2.mtk.hevc.decoder","c2.qti.hevc.decoder","c2.android.hevc.decoder",
                "c2.amlogic.hevc.decoder.secure",null}) {
            assertFalse(DecoderExperimentPolicy.skipLowLatency("MiTV-AYFR0",34,decoder,"video/hevc",true));
        }
        assertTrue(DecoderExperimentPolicy.skipLowLatency("mitv-ayfr0",34,"C2.AMLOGIC.HEVC.DECODER","video/hevc",true));
    }
}
