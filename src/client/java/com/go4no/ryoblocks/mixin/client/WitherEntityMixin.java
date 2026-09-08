package com.go4no.ryoblocks.mixin.client;

import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin {
    @Redirect(
        method = "tickMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle("
                + "Lnet/minecraft/particle/ParticleEffect;"
                + "DDDDDD)V"
        )
    )
    private void ryoBlocks$suppressKitaWitherParticles(
        World world,
        ParticleEffect particle,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ
    ) {
        if (!((Object) this instanceof KitaAngelBossEntity)) {
            world.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}
