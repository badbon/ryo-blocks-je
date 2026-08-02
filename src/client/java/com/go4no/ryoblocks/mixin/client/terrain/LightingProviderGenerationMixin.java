package com.go4no.ryoblocks.mixin.client.terrain;

import com.go4no.ryoblocks.client.terrain.LightingProviderGenerationAccess;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightingProvider.class)
abstract class LightingProviderGenerationMixin implements LightingProviderGenerationAccess {
    @Unique private volatile long ryoBlocks$lightingGeneration;

    @Inject(method = "setColumnEnabled", at = @At("RETURN"))
    private void ryoBlocks$columnEnabledChanged(ChunkPos pos, boolean enabled, CallbackInfo callback) {
        ryoBlocks$lightingGeneration++;
    }

    @Inject(method = "propagateLight", at = @At("RETURN"))
    private void ryoBlocks$columnPropagated(ChunkPos pos, CallbackInfo callback) {
        // ChunkSkyLightProvider.propagateLight directly enables its storage.
        ryoBlocks$lightingGeneration++;
    }

    @Override
    public long ryoBlocks$getLightingGeneration() {
        return ryoBlocks$lightingGeneration;
    }
}
