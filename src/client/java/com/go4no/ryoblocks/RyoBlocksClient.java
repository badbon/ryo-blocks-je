package com.go4no.ryoblocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import com.go4no.ryoblocks.client.KitaAngelBossModel;
import com.go4no.ryoblocks.client.KitaAngelBossModelLayers;
import com.go4no.ryoblocks.client.KitaAngelBossRenderer;
import com.go4no.ryoblocks.client.KitaAngelBossVisualProof;
import com.go4no.ryoblocks.client.NijikaVillagerRenderer;
import com.go4no.ryoblocks.client.PaSanEndermanRenderer;
import com.go4no.ryoblocks.client.KitaLavaVisualProof;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Identifier;

public final class RyoBlocksClient implements ClientModInitializer {
    private static final String BASE_RESOURCE_PACK = "ryo_blocks";
    private static final String RENDERER_COMPAT_RESOURCE_PACK = "ryo_blocks_renderer_compat";

    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        loader.getModContainer(RyoBlocks.MOD_ID).ifPresent(container -> {
            ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier(RyoBlocks.MOD_ID, BASE_RESOURCE_PACK),
                container,
                ResourcePackActivationType.ALWAYS_ENABLED
            );
            if (loader.isModLoaded("iris") || loader.isModLoaded("sodium")) {
                ResourceManagerHelper.registerBuiltinResourcePack(
                    new Identifier(RyoBlocks.MOD_ID, RENDERER_COMPAT_RESOURCE_PACK),
                    container,
                    ResourcePackActivationType.ALWAYS_ENABLED
                );
            }
        });
        EntityRendererRegistry.register(EntityType.VILLAGER, NijikaVillagerRenderer::new);
        EntityRendererRegistry.register(EntityType.WANDERING_TRADER, NijikaVillagerRenderer::new);
        EntityRendererRegistry.register(EntityType.ENDERMAN, PaSanEndermanRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(
            KitaAngelBossModelLayers.KITA_ANGEL_BOSS,
            KitaAngelBossModel::getTexturedModelData
        );
        EntityRendererRegistry.register(RyoBlocks.KITA_ANGEL_BOSS, KitaAngelBossRenderer::new);
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), Fluids.LAVA, Fluids.FLOWING_LAVA);
        boolean bossVisualProof = Boolean.getBoolean("ryoBlocks.bossVisualProof");
        if (Boolean.getBoolean("ryoBlocks.visualProof") && !bossVisualProof) {
            ClientTickEvents.END_CLIENT_TICK.register(new KitaLavaVisualProof());
        }
        if (bossVisualProof) {
            ClientTickEvents.END_CLIENT_TICK.register(new KitaAngelBossVisualProof());
        }
    }
}
