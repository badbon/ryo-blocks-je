package com.go4no.ryoblocks.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** File-only audio opt-in. An exact backend list prevents fallback to hardware. */
public final class ProofRecording {
    public static final boolean ENABLED = Boolean.getBoolean("ryoBlocks.proofRecording");

    private ProofRecording() { }

    public static boolean fileAudioEnabled() {
        return ENABLED && "wave".equals(System.getenv("ALSOFT_DRIVERS"))
            && System.getenv("ALSOFT_CONF") != null;
    }

    public static void frame(int stage, int frame) {
        if (!ENABLED) return;
        try {
            // The wave backend writes real-time PCM. Byte positions also include loading time.
            long bytes = Files.size(Path.of("proof-audio.wav"));
            Files.writeString(Path.of("proof-recording.csv"), stage + "," + frame + ","
                + System.nanoTime() + "," + bytes + "\n",
                frame == 0 ? StandardOpenOption.CREATE_NEW : StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Recording requires the file-only audio backend", e);
        }
    }
}
