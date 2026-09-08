package com.go4no.ryoblocks.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.MathHelper;

public final class KitaAngelBossModel<T extends WitherEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart halo;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public KitaAngelBossModel(ModelPart root) {
        super(RenderLayer::getEntityTranslucent);
        this.root = root;
        this.head = root.getChild("head");
        this.halo = root.getChild("halo");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        Dilation slim = new Dilation(-0.15F);

        root.addChild(
            "head",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .uv(32, 0).cuboid(-4.4F, -8.4F, -4.4F, 8.8F, 8.8F, 8.8F, new Dilation(0.2F)),
            ModelTransform.pivot(0.0F, 1.0F, -0.8F)
        );
        root.addChild(
            "torso",
            ModelPartBuilder.create()
                .uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .uv(16, 32).cuboid(-4.5F, 0.0F, -2.5F, 9.0F, 12.5F, 5.0F, new Dilation(0.18F)),
            ModelTransform.pivot(0.0F, 1.0F, 0.0F)
        );
        root.addChild(
            "back_mantle",
            ModelPartBuilder.create()
                .uv(44, 32).cuboid(-6.0F, 0.0F, -0.9F, 12.0F, 5.0F, 1.8F)
                .uv(44, 40).cuboid(-4.8F, 4.6F, -0.7F, 9.6F, 4.2F, 1.4F),
            ModelTransform.of(0.0F, 2.0F, 3.0F, 0.08F, 0.0F, 0.0F)
        );
        root.addChild(
            "left_arm",
            ModelPartBuilder.create()
                .uv(40, 16).cuboid(-1.0F, -1.0F, -1.6F, 3.0F, 13.0F, 3.0F, slim),
            ModelTransform.of(4.7F, 3.1F, -0.2F, 0.46F, 0.0F, -0.32F)
        );
        root.addChild(
            "right_arm",
            ModelPartBuilder.create()
                .uv(40, 16).mirrored().cuboid(-2.0F, -1.0F, -1.6F, 3.0F, 13.0F, 3.0F, slim),
            ModelTransform.of(-4.7F, 3.1F, -0.2F, 0.46F, 0.0F, 0.32F)
        );
        root.addChild(
            "left_leg",
            ModelPartBuilder.create()
                .uv(16, 48).cuboid(-1.8F, 0.0F, -1.7F, 3.6F, 12.0F, 3.6F, slim)
                .uv(0, 48).cuboid(-2.0F, 11.2F, -2.1F, 4.0F, 1.3F, 4.2F, new Dilation(0.05F)),
            ModelTransform.pivot(1.9F, 13.0F, 0.0F)
        );
        root.addChild(
            "right_leg",
            ModelPartBuilder.create()
                .uv(0, 16).cuboid(-1.8F, 0.0F, -1.7F, 3.6F, 12.0F, 3.6F, slim)
                .uv(0, 53).cuboid(-2.0F, 11.2F, -2.1F, 4.0F, 1.3F, 4.2F, new Dilation(0.05F)),
            ModelTransform.pivot(-1.9F, 13.0F, 0.0F)
        );
        root.addChild(
            "halo",
            ModelPartBuilder.create()
                .uv(0, 16).cuboid(-5.5F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F)
                .uv(0, 18).cuboid(-5.5F, -4.5F, -0.5F, 11.0F, 1.0F, 1.0F)
                .uv(0, 20).cuboid(-5.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F)
                .uv(0, 20).cuboid(4.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F),
            ModelTransform.of(0.0F, -7.4F, 2.2F, -0.16F, 0.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T boss, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.yaw = headYaw * ((float)Math.PI / 180.0F);
        this.head.pitch = headPitch * ((float)Math.PI / 180.0F);

        float hover = MathHelper.sin(animationProgress * 0.08F) * 0.6F;
        this.root.pivotY = -3.0F + hover;
        this.halo.yaw = animationProgress * 0.015F;
        float legSway = MathHelper.sin(animationProgress * 0.06F) * 0.08F;
        this.leftLeg.pitch = legSway;
        this.rightLeg.pitch = -legSway;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
