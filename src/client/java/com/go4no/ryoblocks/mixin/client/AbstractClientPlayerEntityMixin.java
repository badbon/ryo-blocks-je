package com.go4no.ryoblocks.mixin.client;

import com.go4no.ryoblocks.RyoPlayerSkin;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void ryoBlocks$forceSkinTexture(CallbackInfoReturnable<Identifier> callback) {
        callback.setReturnValue(RyoPlayerSkin.TEXTURE);
    }

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void ryoBlocks$forceSkinModel(CallbackInfoReturnable<String> callback) {
        callback.setReturnValue(RyoPlayerSkin.MODEL);
    }
}
