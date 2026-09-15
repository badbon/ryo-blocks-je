package com.go4no.ryoblocks.mixin;

import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WitherEntity.class)
public interface WitherAttackCooldownAccessor {
    @Accessor("skullCooldowns")
    int[] ryoBlocks$getSkullCooldowns();
}
