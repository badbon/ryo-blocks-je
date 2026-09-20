package com.go4no.ryoblocks;

import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import com.go4no.ryoblocks.entity.AngelBossEntity;
import com.go4no.ryoblocks.entity.NijikaAngelBossEntity;
import com.go4no.ryoblocks.entity.KikuriAngelBossEntity;
import com.go4no.ryoblocks.entity.KikuriClearingPotionEntity;
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
    public static EntityType<NijikaAngelBossEntity> NIJIKA_ANGEL_BOSS;
    public static EntityType<KikuriAngelBossEntity> KIKURI_ANGEL_BOSS;
    public static EntityType<KikuriClearingPotionEntity> KIKURI_CLEARING_POTION;
    public static SpawnEggItem NIJIKA_ANGEL_BOSS_SPAWN_EGG;
    public static SpawnEggItem KIKURI_ANGEL_BOSS_SPAWN_EGG;
    public static Item DORITO;

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
        NIJIKA_ANGEL_BOSS = registerAngel("nijika_angel_boss", NijikaAngelBossEntity::new);
        KIKURI_ANGEL_BOSS = registerAngel("kikuri_angel_boss", KikuriAngelBossEntity::new);
        NIJIKA_ANGEL_BOSS_SPAWN_EGG = Registry.register(Registries.ITEM, id("nijika_angel_boss_spawn_egg"),
            new SpawnEggItem(NIJIKA_ANGEL_BOSS, 0xEFCB4B, 0xFFFFFF, new Item.Settings()));
        KIKURI_ANGEL_BOSS_SPAWN_EGG = Registry.register(Registries.ITEM, id("kikuri_angel_boss_spawn_egg"),
            new SpawnEggItem(KIKURI_ANGEL_BOSS, 0x854866, 0xFFF0A6, new Item.Settings()));
        KIKURI_CLEARING_POTION = Registry.register(Registries.ENTITY_TYPE, id("kikuri_clearing_potion"),
            FabricEntityTypeBuilder.<KikuriClearingPotionEntity>create(SpawnGroup.MISC, KikuriClearingPotionEntity::new)
                .dimensions(EntityDimensions.fixed(0.25F, 0.25F)).trackRangeChunks(8).trackedUpdateRate(1).build());
        DORITO = Registry.register(Registries.ITEM, id("dorito"), new Item(new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(KITA_ANGEL_BOSS_SPAWN_EGG);
            entries.add(NIJIKA_ANGEL_BOSS_SPAWN_EGG);
            entries.add(KIKURI_ANGEL_BOSS_SPAWN_EGG);
        });
    }

    private static <T extends AngelBossEntity> EntityType<T> registerAngel(String name, EntityType.EntityFactory<T> factory) {
        EntityType<T> type = Registry.register(Registries.ENTITY_TYPE, id(name),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, factory)
                .dimensions(EntityDimensions.fixed(0.9F, 3.5F)).trackRangeChunks(10).trackedUpdateRate(3).build());
        FabricDefaultAttributeRegistry.register(type, WitherEntity.createWitherAttributes());
        return type;
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
