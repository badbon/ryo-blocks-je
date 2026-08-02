package com.go4no.ryoblocks.mixin.client.terrain;

import com.go4no.ryoblocks.client.terrain.TerrainBatcher;
import com.go4no.ryoblocks.client.terrain.LongBooleanMemo;
import com.go4no.ryoblocks.client.terrain.LightingProviderGenerationAccess;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererTerrainBatchMixin {
    @Shadow @Final private ObjectArrayList<?> chunkInfos;
    @Unique private LongBooleanMemo ryoBlocks$lightingColumns;

    @Redirect(
        method = "updateChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/LightingProvider;isLightingEnabled(Lnet/minecraft/util/math/ChunkSectionPos;)Z"
        )
    )
    private boolean ryoBlocks$isLightingEnabledByColumn(LightingProvider provider, ChunkSectionPos sectionPos) {
        LongBooleanMemo memo = ryoBlocks$lightingColumns;
        if (memo == null) {
            memo = new LongBooleanMemo();
            ryoBlocks$lightingColumns = memo;
        }
        if (!(provider instanceof LightingProviderGenerationAccess generationAccess)) {
            return provider.isLightingEnabled(sectionPos);
        }
        long columnPos = ChunkSectionPos.withZeroY(sectionPos.asLong());
        long generation = generationAccess.ryoBlocks$getLightingGeneration();
        return memo.getOrCompute(provider, generation, columnPos, ignored -> provider.isLightingEnabled(sectionPos));
    }

    @Inject(method = "addBuiltChunk", at = @At("HEAD"))
    private void ryoBlocks$commitBatch(ChunkBuilder.BuiltChunk chunk, CallbackInfo callback) {
        TerrainBatcher.get().commitChunk(chunk);
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    private void ryoBlocks$renderBatch(
        RenderLayer layer,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ,
        Matrix4f projectionMatrix,
        CallbackInfo callback
    ) {
        TerrainBatcher batcher = TerrainBatcher.get();
        if (layer == RenderLayer.getSolid()) {
            batcher.beginFrame(chunkInfos);
        }
        if (batcher.render(layer, matrices, cameraX, cameraY, cameraZ, projectionMatrix)) {
            callback.cancel();
        }
    }

    @Inject(method = "reload()V", at = @At("HEAD"))
    private void ryoBlocks$resetBatchOnReload(CallbackInfo callback) {
        TerrainBatcher.get().reset();
        ryoBlocks$resetLightingMemo();
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void ryoBlocks$resetBatchOnWorldChange(net.minecraft.client.world.ClientWorld world, CallbackInfo callback) {
        TerrainBatcher.get().reset();
        ryoBlocks$resetLightingMemo();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void ryoBlocks$resetBatchOnClose(CallbackInfo callback) {
        TerrainBatcher.get().reset();
        ryoBlocks$resetLightingMemo();
    }

    @Unique
    private void ryoBlocks$resetLightingMemo() {
        if (ryoBlocks$lightingColumns != null) {
            ryoBlocks$lightingColumns.reset();
        }
    }
}
