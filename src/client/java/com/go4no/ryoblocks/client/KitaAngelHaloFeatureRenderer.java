package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;

public final class KitaAngelHaloFeatureRenderer<T extends WitherEntity> extends FeatureRenderer<T, KitaAngelBossModel<T>> {
    public KitaAngelHaloFeatureRenderer(FeatureRendererContext<T, KitaAngelBossModel<T>> context) {
        super(context);
    }

    @Override
    public void render(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        T wither,
        float limbAngle,
        float limbDistance,
        float tickDelta,
        float animationProgress,
        float headYaw,
        float headPitch
    ) {
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(KitaAngelBossAssets.HALO_TEXTURE));
        this.getContextModel().renderHalo(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
    }
}
