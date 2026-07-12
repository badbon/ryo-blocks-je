package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.NijikaAssets;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public final class NijikaVillagerRenderer<T extends MobEntity> extends MobEntityRenderer<T, PlayerEntityModel<T>> {
    public NijikaVillagerRenderer(EntityRendererFactory.Context context) {
        super(
            context,
            new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), NijikaAssets.SLIM_ARMS),
            0.5F
        );
    }

    @Override
    public Identifier getTexture(T entity) {
        return NijikaAssets.PLAYER_TEXTURE;
    }
}
