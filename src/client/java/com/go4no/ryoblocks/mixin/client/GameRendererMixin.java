package com.go4no.ryoblocks.mixin.client;

import com.go4no.ryoblocks.client.RyoTerrainShaders;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "loadPrograms", at = @At("TAIL"))
    private void ryoBlocks$bindTerrainOverlaySampler(ResourceFactory factory, CallbackInfo ci) {
        RyoTerrainShaders.bindTerrainSamplers();
    }
}
