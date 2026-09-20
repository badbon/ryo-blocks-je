package com.go4no.ryoblocks.entity;

import com.go4no.ryoblocks.RyoBlocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class KikuriAngelBossEntity extends AngelBossEntity {
    public KikuriAngelBossEntity(EntityType<? extends WitherEntity> type, World world) {
        super(type, world);
    }

    @Override
    public boolean shootVariantProjectile(int head, double x, double y, double z, boolean charged) {
        Potion potion = Potions.HARMING;
        var tracked = getWorld().getEntityById(getTrackedEntityId(head == 0 ? 0 : head - 1));
        if (!charged && tracked instanceof LivingEntity target) {
            double distance = Math.hypot(x - getX(), z - getZ());
            // Vanilla Witch offensive selection, without adding its unrelated drinking/raid AI.
            if (distance >= 8 && !target.hasStatusEffect(StatusEffects.SLOWNESS)) potion = Potions.SLOWNESS;
            else if (target.getHealth() >= 8 && !target.hasStatusEffect(StatusEffects.POISON)) potion = Potions.POISON;
            else if (distance <= 3 && !target.hasStatusEffect(StatusEffects.WEAKNESS) && random.nextFloat() < 0.25F)
                potion = Potions.WEAKNESS;
            x += target.getVelocity().x;
            z += target.getVelocity().z;
        }
        PotionEntity bottle = new PotionEntity(getWorld(), this);
        bottle.setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        double dx = x - bottle.getX();
        double dz = z - bottle.getZ();
        bottle.setVelocity(dx, y - bottle.getY() + Math.hypot(dx, dz) * 0.2, dz, 0.75F, 8.0F);
        getWorld().spawnEntity(bottle);
        playClearingSound();
        return true;
    }

    @Override
    protected void shootObstacleProjectile(BlockPos obstacle) {
        var bottle = new KikuriClearingPotionEntity(RyoBlocks.KIKURI_CLEARING_POTION, getWorld());
        bottle.setOwner(this);
        bottle.setPosition(getEyePos());
        bottle.setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.HARMING));
        bottle.setNoGravity(true);
        bottle.setVelocity(obstacle.toCenterPos().subtract(getEyePos()).normalize().multiply(0.8));
        bottle.addCommandTag(OBSTACLE_SHOT_TAG);
        getWorld().spawnEntity(bottle);
    }

    @Override
    protected void playClearingSound() {
        if (!isSilent()) getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITCH_THROW,
            getSoundCategory(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
    }
}
