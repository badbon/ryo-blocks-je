package com.go4no.ryoblocks;

import com.go4no.ryoblocks.entity.PaSanEndermanEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.Heightmap;

public final class RyoBlocks implements ModInitializer {
    public static final String MOD_ID = "ryo-blocks";
    public static final EntityType<PaSanEndermanEntity> PA_SAN_ENDERMAN = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(MOD_ID, "pa_san_enderman"),
        EntityType.Builder.create(PaSanEndermanEntity::new, SpawnGroup.MONSTER)
            .setDimensions(0.6F, 2.9F)
            .maxTrackingRange(8)
            .build(MOD_ID + ":pa_san_enderman")
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(PA_SAN_ENDERMAN, EndermanEntity.createEndermanAttributes());
        SpawnRestriction.register(
            PA_SAN_ENDERMAN,
            SpawnRestriction.Location.ON_GROUND,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            HostileEntity::canSpawnInDark
        );

        BiomeModifications.create(new Identifier(MOD_ID, "replace_enderman_spawns"))
            .add(ModificationPhase.REPLACEMENTS, BiomeSelectors.all(), RyoBlocks::replaceEndermanSpawns);
    }

    private static void replaceEndermanSpawns(
        net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext selection,
        BiomeModificationContext modification
    ) {
        BiomeModificationContext.SpawnSettingsContext spawns = modification.getSpawnSettings();
        List<SpawnReplacement> replacements = new ArrayList<>();

        spawns.removeSpawns((group, entry) -> {
            if (entry.type != EntityType.ENDERMAN) {
                return false;
            }

            replacements.add(new SpawnReplacement(group, entry));
            return true;
        });

        for (SpawnReplacement replacement : replacements) {
            SpawnSettings.SpawnEntry entry = replacement.entry();
            spawns.addSpawn(
                replacement.group(),
                new SpawnSettings.SpawnEntry(
                    PA_SAN_ENDERMAN,
                    entry.getWeight(),
                    entry.minGroupSize,
                    entry.maxGroupSize
                )
            );
        }

        SpawnSettings.SpawnDensity density = selection.getBiome().getSpawnSettings().getSpawnDensity(EntityType.ENDERMAN);
        if (density != null) {
            spawns.clearSpawnCost(EntityType.ENDERMAN);
            spawns.setSpawnCost(PA_SAN_ENDERMAN, density.mass(), density.gravityLimit());
        }
    }

    private record SpawnReplacement(SpawnGroup group, SpawnSettings.SpawnEntry entry) {
    }
}
