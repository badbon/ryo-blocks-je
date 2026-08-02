package com.go4no.ryoblocks.mixin.client.terrain;

import com.go4no.ryoblocks.client.terrain.TerrainBatcher;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
abstract class VertexBufferTerrainBatchMixin {
    @Inject(method = "upload", at = @At("HEAD"))
    private void ryoBlocks$mirrorUpload(BufferBuilder.BuiltBuffer buffer, CallbackInfo callback) {
        TerrainBatcher.get().mirrorUpload((VertexBuffer) (Object) this, buffer);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void ryoBlocks$untrackBuffer(CallbackInfo callback) {
        TerrainBatcher.get().untrackBuffer((VertexBuffer) (Object) this);
    }
}
