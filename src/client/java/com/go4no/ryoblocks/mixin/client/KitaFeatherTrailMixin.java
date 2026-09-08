package com.go4no.ryoblocks.mixin.client;

import com.go4no.ryoblocks.client.KitaFeatherEffects;
import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExplosiveProjectileEntity.class)
public abstract class KitaFeatherTrailMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"))
    private void ryoBlocks$featherTrail(World world, ParticleEffect particle, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
        if (particle == ParticleTypes.SMOKE && (Object)this instanceof WitherSkullEntity skull
            && skull.getOwner() instanceof KitaAngelBossEntity) {
            world.addParticle(skull.age % 2 == 0 ? KitaFeatherEffects.GOLD : KitaFeatherEffects.WHITE,
                x, y - 0.5, z, velocityX, velocityY, velocityZ);
        } else {
            world.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}
