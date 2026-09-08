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
import net.minecraft.util.math.MathHelper;

public final class KitaAngelWingsFeatureRenderer<T extends WitherEntity> extends FeatureRenderer<T, KitaAngelBossModel<T>> {
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public KitaAngelWingsFeatureRenderer(FeatureRendererContext<T, KitaAngelBossModel<T>> context) {
        super(context);
        ModelPart root = getTexturedModelData().createModel();
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData leftWing = root.addChild(
            "left_wing",
            ModelPartBuilder.create(),
            ModelTransform.of(3.9F, 1.0F, 3.25F, 0.02F, -0.38F, -0.16F)
        );
        addWingFeathers(leftWing, false);

        ModelPartData rightWing = root.addChild(
            "right_wing",
            ModelPartBuilder.create(),
            ModelTransform.of(-3.9F, 1.0F, 3.25F, 0.02F, 0.38F, 0.16F)
        );
        addWingFeathers(rightWing, true);

        return TexturedModelData.of(modelData, 64, 64);
    }

    private static void addWingFeathers(ModelPartData wingRoot, boolean rightSide) {
        float sign = rightSide ? -1.0F : 1.0F;
        wingRoot.addChild(
            "shoulder_coverts",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(rightSide ? -5.2F : 0.0F, -2.6F, -1.35F, 5.2F, 5.8F, 2.6F)
                .uv(16, 0).cuboid(rightSide ? -4.0F : 0.0F, -3.4F, -0.95F, 4.0F, 4.0F, 1.8F),
            ModelTransform.of(0.0F, 0.0F, 0.0F, -0.08F, 0.0F, sign * -0.04F)
        );
        wingRoot.addChild(
            "upper_spar",
            ModelPartBuilder.create()
                .uv(0, 8).cuboid(rightSide ? -15.5F : 0.0F, -1.2F, -0.75F, 15.5F, 2.4F, 1.5F),
            ModelTransform.of(sign * 1.8F, -2.4F, 0.0F, 0.0F, sign * -0.05F, sign * -0.28F)
        );

        for (int index = 0; index < 7; index++) {
            float x = sign * (3.0F + index * 2.0F);
            float y = -0.7F + index * 0.8F;
            float length = 8.8F + index * 1.15F;
            float width = 2.35F - index * 0.06F;
            float roll = sign * (0.28F + index * 0.075F);
            String name = "primary_" + index;
            wingRoot.addChild(
                name,
                ModelPartBuilder.create()
                    .uv(0, 16 + (index % 3) * 8)
                    .cuboid(rightSide ? -length : 0.0F, 0.0F, -0.55F, length, width, 1.1F),
                ModelTransform.of(x, y, 0.22F + index * 0.05F, 0.0F, sign * -0.14F, roll)
            );
        }

        for (int index = 0; index < 5; index++) {
            float x = sign * (2.4F + index * 1.65F);
            float y = -4.2F + index * 0.58F;
            float length = 7.0F + index * 0.9F;
            wingRoot.addChild(
                "upper_feather_" + index,
                ModelPartBuilder.create()
                    .uv(24, 16 + (index % 2) * 8)
                    .cuboid(rightSide ? -length : 0.0F, 0.0F, -0.5F, length, 1.55F, 1.0F),
                ModelTransform.of(x, y, 0.2F, 0.0F, sign * -0.12F, sign * (-0.36F - index * 0.07F))
            );
        }

        for (int index = 0; index < 3; index++) {
            float length = 6.2F + index * 1.2F;
            wingRoot.addChild(
                "inner_covert_" + index,
                ModelPartBuilder.create()
                    .uv(36, 32 + index * 6)
                    .cuboid(rightSide ? -length : 0.0F, 0.0F, -0.6F, length, 2.1F, 1.2F),
                ModelTransform.of(sign * (2.2F + index * 1.35F), 1.0F + index * 0.72F, -0.15F, 0.0F, sign * -0.08F, sign * (0.04F + index * 0.09F))
            );
        }
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
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(KitaAngelBossAssets.WINGS_TEXTURE));
        float flap = MathHelper.sin(animationProgress * 0.12F) * 0.08F;
        this.leftWing.yaw = -0.38F - flap;
        this.leftWing.roll = -0.16F - flap * 0.45F;
        this.rightWing.yaw = 0.38F + flap;
        this.rightWing.roll = 0.16F + flap * 0.45F;
        this.leftWing.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
        this.rightWing.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
    }
}
