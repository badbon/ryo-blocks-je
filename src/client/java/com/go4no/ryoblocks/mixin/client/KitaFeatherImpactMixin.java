package com.go4no.ryoblocks.mixin.client;

import com.go4no.ryoblocks.client.KitaFeatherEffects;
import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullEntity.class)
public abstract class KitaFeatherImpactMixin {
    @Unique private boolean ryoBlocks$impactShown;

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void ryoBlocks$featherImpact(HitResult hit, CallbackInfo ci) {
        WitherSkullEntity skull = (WitherSkullEntity)(Object)this;
        if (skull.getWorld().isClient && !ryoBlocks$impactShown && skull.getOwner() instanceof KitaAngelBossEntity) {
            ryoBlocks$impactShown = true;
            KitaFeatherEffects.impact(skull.getWorld(), hit.getPos(), skull.isCharged());
        }
    }
}
