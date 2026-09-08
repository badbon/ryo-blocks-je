package com.go4no.ryoblocks.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class KitaAngelBossEntity extends WitherEntity {
    public KitaAngelBossEntity(EntityType<? extends WitherEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (!getWorld().isClient && !isAiDisabled() && getTrackedEntityId(0) > 0 && getInvulnerableTimer() == 0
            && !isTouchingWater() && !isInLava()) {
            var target = getWorld().getEntityById(getTrackedEntityId(0));
            if (target != null && target.isAlive()) {
                // Replace the Wither's abrupt lift threshold with a gentle approach to the same altitude.
                double altitude = target.getY() + (shouldRenderOverlay() ? 0.0 : 5.0);
                double verticalSpeed = MathHelper.clamp((altitude - getY()) * 0.05, -0.10, 0.15);
                Vec3d velocity = getVelocity();
                setVelocity(velocity.x, verticalSpeed, velocity.z);
            }
        }
        super.travel(movementInput);
    }
}
