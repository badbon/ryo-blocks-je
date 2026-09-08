package com.go4no.ryoblocks;

import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class RyoBlocks implements ModInitializer {
    public static final String MOD_ID = "ryo-blocks";
    public static EntityType<KitaAngelBossEntity> KITA_ANGEL_BOSS;
    public static SpawnEggItem KITA_ANGEL_BOSS_SPAWN_EGG;

    public RyoBlocks() {
    }

    @Override
    public void onInitialize() {
        KITA_ANGEL_BOSS = Registry.register(
            Registries.ENTITY_TYPE,
            id("kita_angel_boss"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, KitaAngelBossEntity::new)
                .dimensions(EntityDimensions.fixed(0.9F, 3.5F))
                .trackRangeChunks(10)
                .trackedUpdateRate(3)
                .build()
        );
        KITA_ANGEL_BOSS_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("kita_angel_boss_spawn_egg"),
            new SpawnEggItem(KITA_ANGEL_BOSS, 0xE986B8, 0xFFF0A6, new Item.Settings())
        );
        FabricDefaultAttributeRegistry.register(KITA_ANGEL_BOSS, WitherEntity.createWitherAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries ->
            entries.add(KITA_ANGEL_BOSS_SPAWN_EGG)
        );
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
