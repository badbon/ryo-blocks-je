package com.go4no.ryoblocks.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/** Only obstacle-clearing bottles explode; regular combat uses unmodified vanilla splash potions. */
public final class KikuriClearingPotionEntity extends PotionEntity {
    public KikuriClearingPotionEntity(EntityType<? extends PotionEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void onCollision(HitResult hit) {
        if (!getWorld().isClient) {
            setPosition(hit.getPos());
            getWorld().createExplosion(this, getX(), getY(), getZ(), 1.0F, false, World.ExplosionSourceType.MOB);
        }
        super.onCollision(hit);
    }

    @Override
    public float getEffectiveExplosionResistance(Explosion explosion, BlockView world, BlockPos pos,
                                                 BlockState block, FluidState fluid, float resistance) {
        float base = super.getEffectiveExplosionResistance(explosion, world, pos, block, fluid, resistance);
        return WitherEntity.canDestroy(block) ? Math.min(0.8F, base) : base;
    }
}
