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
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class KitaAngelWingsFeatureRenderer<T extends WitherEntity> extends FeatureRenderer<T, KitaAngelBossModel<T>> {
    private static final float WING_HALF_WIDTH = 23.5F;
    private static final float WING_TOP = -10.2F;
    private static final float WING_BOTTOM = 15.0F;
    private static final float WING_ROOT_Z = 3.1F;
    private static final float WING_TIP_Z = 8.8F;

    public KitaAngelWingsFeatureRenderer(FeatureRendererContext<T, KitaAngelBossModel<T>> context) {
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
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(KitaAngelBossAssets.WINGS_TEXTURE));
        matrices.push();
        matrices.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        float flap = (float)Math.sin(animationProgress * 0.12F) * 1.1F;
        float yTop = WING_TOP + flap * 0.18F;
        float yBottom = WING_BOTTOM - flap * 0.1F;
        drawWingHalf(vertices, position, normal, -WING_HALF_WIDTH, 0.0F, yTop, yBottom, 0.0F, 0.5F, light);
        drawWingHalf(vertices, position, normal, 0.0F, WING_HALF_WIDTH, yTop, yBottom, 0.5F, 1.0F, light);
        matrices.pop();
    }

    private static void drawWingHalf(
        VertexConsumer vertices,
        Matrix4f position,
        Matrix3f normal,
        float left,
        float right,
        float top,
        float bottom,
        float uLeft,
        float uRight,
        int light
    ) {
        float leftZ = left < 0.0F ? WING_TIP_Z : WING_ROOT_Z;
        float rightZ = right > 0.0F ? WING_TIP_Z : WING_ROOT_Z;
        vertex(vertices, position, normal, left, bottom, leftZ, uLeft, 1.0F, light);
        vertex(vertices, position, normal, right, bottom, rightZ, uRight, 1.0F, light);
        vertex(vertices, position, normal, right, top, rightZ, uRight, 0.0F, light);
        vertex(vertices, position, normal, left, top, leftZ, uLeft, 0.0F, light);

        vertex(vertices, position, normal, left, top, leftZ + 0.02F, uLeft, 0.0F, light);
        vertex(vertices, position, normal, right, top, rightZ + 0.02F, uRight, 0.0F, light);
        vertex(vertices, position, normal, right, bottom, rightZ + 0.02F, uRight, 1.0F, light);
        vertex(vertices, position, normal, left, bottom, leftZ + 0.02F, uLeft, 1.0F, light);
    }

    private static void vertex(
        VertexConsumer vertices,
        Matrix4f position,
        Matrix3f normal,
        float x,
        float y,
        float z,
        float u,
        float v,
        int light
    ) {
        vertices.vertex(position, x, y, z)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal, 0.0F, 0.0F, -1.0F)
            .next();
    }
}
