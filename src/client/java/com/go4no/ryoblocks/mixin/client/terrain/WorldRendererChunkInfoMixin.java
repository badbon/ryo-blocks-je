package com.go4no.ryoblocks.mixin.client.terrain;

import com.go4no.ryoblocks.client.terrain.WorldRendererChunkInfoAccess;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
abstract class WorldRendererChunkInfoMixin implements WorldRendererChunkInfoAccess {
    @Override
    @Accessor("chunk")
    public abstract ChunkBuilder.BuiltChunk ryoBlocks$getChunk();
}
