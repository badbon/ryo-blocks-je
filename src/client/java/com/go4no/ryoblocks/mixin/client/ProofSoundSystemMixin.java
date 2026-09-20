package com.go4no.ryoblocks.mixin.client;

import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public abstract class ProofSoundSystemMixin {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void ryoBlocks$disableProofAudioDevice(CallbackInfo ci) {
        // Only an explicit file-only backend may bypass the proof speaker guard.
        if (Boolean.getBoolean("ryoBlocks.visualProof")
            && !com.go4no.ryoblocks.client.ProofRecording.fileAudioEnabled()) {
            org.slf4j.LoggerFactory.getLogger("RyoBlocks/ProofAudio").info("Proof audio device disabled before startup");
            ci.cancel();
        }
    }
}
