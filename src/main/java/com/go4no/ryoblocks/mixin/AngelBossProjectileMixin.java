package com.go4no.ryoblocks.mixin;

import com.go4no.ryoblocks.entity.AngelBossEntity;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public abstract class AngelBossProjectileMixin {
    @Inject(method = "shootSkullAt(IDDDZ)V", at = @At("HEAD"), cancellable = true)
    private void ryoBlocks$variantShot(int head, double x, double y, double z, boolean charged, CallbackInfo ci) {
        if ((Object)this instanceof AngelBossEntity boss && boss.shootVariantProjectile(head, x, y, z, charged)) {
            ci.cancel();
        }
    }
}
