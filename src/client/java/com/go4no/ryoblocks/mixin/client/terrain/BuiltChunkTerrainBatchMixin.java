package com.go4no.ryoblocks.mixin.client.terrain;

import com.go4no.ryoblocks.client.terrain.TerrainBatcher;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilder.BuiltChunk.class)
abstract class BuiltChunkTerrainBatchMixin {
    @Inject(method = "getBuffer", at = @At("RETURN"))
    private void ryoBlocks$trackBuffer(RenderLayer layer, CallbackInfoReturnable<VertexBuffer> callback) {
        TerrainBatcher.get().trackBuffer((ChunkBuilder.BuiltChunk) (Object) this, layer, callback.getReturnValue());
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void ryoBlocks$removeBatchData(CallbackInfo callback) {
        TerrainBatcher.get().removeChunk((ChunkBuilder.BuiltChunk) (Object) this);
    }
}
