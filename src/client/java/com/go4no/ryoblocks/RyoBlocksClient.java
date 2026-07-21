package com.go4no.ryoblocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import com.go4no.ryoblocks.client.NijikaVillagerRenderer;
import com.go4no.ryoblocks.client.PaSanEndermanRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public final class RyoBlocksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricLoader.getInstance().getModContainer(RyoBlocks.MOD_ID).ifPresent(container ->
            ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier(RyoBlocks.MOD_ID, "ryo_blocks"),
                container,
                ResourcePackActivationType.ALWAYS_ENABLED
            )
        );
        EntityRendererRegistry.register(EntityType.VILLAGER, NijikaVillagerRenderer::new);
        EntityRendererRegistry.register(EntityType.WANDERING_TRADER, NijikaVillagerRenderer::new);
        EntityRendererRegistry.register(EntityType.ENDERMAN, PaSanEndermanRenderer::new);
        EntityRendererRegistry.register(RyoBlocks.PA_SAN_ENDERMAN, PaSanEndermanRenderer::new);
    }
}
