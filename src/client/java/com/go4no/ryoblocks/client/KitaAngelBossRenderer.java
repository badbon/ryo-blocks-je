package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;

public final class KitaAngelBossRenderer<T extends WitherEntity> extends MobEntityRenderer<T, PlayerEntityModel<T>> {
    private static final float BOSS_SCALE = 2.45F;

    public KitaAngelBossRenderer(EntityRendererFactory.Context context) {
        super(
            context,
            new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), KitaAngelBossAssets.SLIM_ARMS),
            1.0F
        );
        this.addFeature(new KitaAngelWingsFeatureRenderer<>(this));
    }

    @Override
    public void render(
        T wither,
        float entityYaw,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light
    ) {
        matrices.push();
        matrices.scale(BOSS_SCALE, BOSS_SCALE, BOSS_SCALE);
        super.render(wither, entityYaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    public Identifier getTexture(T wither) {
        return KitaAngelBossAssets.PLAYER_TEXTURE;
    }
}
