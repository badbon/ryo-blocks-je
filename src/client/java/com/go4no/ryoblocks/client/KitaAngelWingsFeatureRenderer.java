package com.go4no.ryoblocks.client;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;

public final class KitaAngelWingsFeatureRenderer<T extends WitherEntity> extends FeatureRenderer<T, KitaAngelBossModel<T>> {
    private static final Identifier FEATHERS = new Identifier("minecraft", "textures/block/snow.png");
    private final ModelPart left = createWing();
    private final ModelPart right = createWing();

    public KitaAngelWingsFeatureRenderer(FeatureRendererContext<T, KitaAngelBossModel<T>> context) {
        super(context);
    }

    private static ModelPart createWing() {
        ModelData data = new ModelData();
        ModelPartData shoulder = data.getRoot().addChild("shoulder", ModelPartBuilder.create(), ModelTransform.NONE);
        feather(shoulder, "root", 0, 0, 0, 4, 9, 3, -0.45F);
        feather(shoulder, "inner0", 2, 0, 0.4F, 3, 16, 2, -0.26F);
        feather(shoulder, "inner1", 4.5F, -2, 0.7F, 3, 18, 2, -0.32F);
        feather(shoulder, "inner2", 7, -4, 1, 3, 19, 2, -0.38F);
        ModelPartData outer = shoulder.addChild("outer", ModelPartBuilder.create(), ModelTransform.pivot(9, -5, 1));
        // Flight feathers climb into an arch, with squared steps along the trailing edge.
        for (int i = 0; i < 5; i++) {
            feather(outer, "flight" + i, i * 2.5F, -i * 2.1F, i * 0.25F,
                3, 19 - i * 1.8F, 2, -0.40F - i * 0.10F);
            feather(outer, "middle" + i, i * 2.5F + 0.2F, -i * 2.1F + 2, -0.5F + i * 0.25F,
                3.2F, 10 - i * 0.6F, 2.6F, -0.34F - i * 0.10F);
            feather(outer, "cover" + i, i * 2.5F - 0.3F, -i * 2.1F - 0.6F, -0.8F + i * 0.25F,
                3.4F, 6.5F, 3.2F, -0.40F - i * 0.10F);
        }
        for (int i = 0; i < 3; i++) {
            feather(shoulder, "down" + i, 1 + i * 2.6F, -1 - i * 1.8F, -0.8F,
                3.5F, 6.5F, 3.4F, -0.35F);
        }
        return TexturedModelData.of(data, 16, 16).createModel().getChild("shoulder");
    }

    private static void feather(ModelPartData parent, String name, float x, float y, float z,
                                float width, float length, float depth, float angle) {
        parent.addChild(name, ModelPartBuilder.create()
            .uv(0, 0).cuboid(0, 0, -depth / 2, width, length - 2, depth)
            .uv(0, 0).cuboid(0.5F, length - 2, -depth / 2, width - 1, 2, depth),
            ModelTransform.of(x, y, z, 0, 0, angle));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T wither,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        float breath = (float)Math.sin(animationProgress * 0.055F);
        renderWing(matrices, vertexConsumers, light, left, 1, breath);
        renderWing(matrices, vertexConsumers, light, right, -1, breath);
    }

    private void renderWing(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                            ModelPart wing, int side, float breath) {
        matrices.push();
        getContextModel().body.rotate(matrices);
        matrices.translate(side * 2.0F / 16, 1.0F / 16, 3.0F / 16);
        matrices.scale(side, 1, 1);
        wing.yaw = -0.24F + breath * 0.045F;
        wing.roll = breath * 0.018F;
        wing.getChild("outer").yaw = -0.12F + breath * 0.035F;
        wing.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(FEATHERS)),
            light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
