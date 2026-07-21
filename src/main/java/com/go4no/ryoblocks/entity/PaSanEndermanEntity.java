package com.go4no.ryoblocks.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * A registered Enderman subtype. Its behavior is inherited directly from
 * EndermanEntity so AI, teleporting, combat, carrying, sounds, drops, and NBT
 * stay identical to vanilla.
 */
public final class PaSanEndermanEntity extends EndermanEntity {
    public PaSanEndermanEntity(EntityType<? extends PaSanEndermanEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected Identifier getLootTableId() {
        return EntityType.ENDERMAN.getLootTableId();
    }
}
