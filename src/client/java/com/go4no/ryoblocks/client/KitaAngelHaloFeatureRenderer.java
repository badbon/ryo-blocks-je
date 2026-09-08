package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;

public final class KitaAngelHaloFeatureRenderer<T extends WitherEntity> extends FeatureRenderer<T, KitaAngelBossModel<T>> {
    private final ModelPart halo;

    public KitaAngelHaloFeatureRenderer(FeatureRendererContext<T, KitaAngelBossModel<T>> context) {
        super(context);
        this.halo = getTexturedModelData().createModel().getChild("halo");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild(
            "halo",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-5.5F, -0.5F, -3.5F, 11.0F, 1.0F, 1.0F)
                .uv(0, 0).cuboid(-5.5F, -0.5F, 2.5F, 11.0F, 1.0F, 1.0F)
                .uv(0, 0).cuboid(-5.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F)
                .uv(0, 0).cuboid(4.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F),
            ModelTransform.pivot(0.0F, -9.4F, 0.0F)
        );
        return TexturedModelData.of(modelData, 16, 16);
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
        this.halo.yaw = animationProgress * 0.015F;
        this.halo.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
    }
}
