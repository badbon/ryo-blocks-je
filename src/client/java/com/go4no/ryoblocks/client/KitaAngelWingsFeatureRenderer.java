package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class KitaAngelWingsFeatureRenderer<T extends WitherEntity> extends FeatureRenderer<T, PlayerEntityModel<T>> {
    private static final float WING_WIDTH = 1.45F;
    private static final float WING_HEIGHT = 1.15F;
    private static final float WING_Y = 0.35F;
    private static final float WING_Z = 0.21F;

    public KitaAngelWingsFeatureRenderer(FeatureRendererContext<T, PlayerEntityModel<T>> context) {
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
        float flap = MathHelper.sin(animationProgress * 0.16F) * 4.0F;
        renderWing(matrices, vertices, light, false, -18.0F - flap);
        renderWing(matrices, vertices, light, true, 18.0F + flap);
    }

    private static void renderWing(
        MatrixStack matrices,
        VertexConsumer vertices,
        int light,
        boolean mirrored,
        float yawDegrees
    ) {
        matrices.push();
        matrices.translate(0.0F, WING_Y, WING_Z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDegrees));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mirrored ? -6.0F : 6.0F));

        float innerX = mirrored ? 0.02F : -0.02F;
        float outerX = mirrored ? WING_WIDTH : -WING_WIDTH;
        float topY = -0.95F;
        float bottomY = topY + WING_HEIGHT;
        float u0 = mirrored ? 0.5F : 0.0F;
        float u1 = mirrored ? 1.0F : 0.5F;

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();
        drawQuad(vertices, position, normal, light, innerX, topY, outerX, topY, outerX, bottomY, innerX, bottomY, u0, 0.0F, u1, 1.0F, -1.0F);
        drawQuad(vertices, position, normal, light, innerX, bottomY, outerX, bottomY, outerX, topY, innerX, topY, u0, 1.0F, u1, 0.0F, 1.0F);
        matrices.pop();
    }

    private static void drawQuad(
        VertexConsumer vertices,
        Matrix4f position,
        Matrix3f normal,
        int light,
        float x0,
        float y0,
        float x1,
        float y1,
        float x2,
        float y2,
        float x3,
        float y3,
        float u0,
        float v0,
        float u1,
        float v1,
        float normalZ
    ) {
        vertex(vertices, position, normal, light, x0, y0, 0.0F, u0, v0, normalZ);
        vertex(vertices, position, normal, light, x1, y1, 0.0F, u1, v0, normalZ);
        vertex(vertices, position, normal, light, x2, y2, 0.0F, u1, v1, normalZ);
        vertex(vertices, position, normal, light, x3, y3, 0.0F, u0, v1, normalZ);
    }

    private static void vertex(
        VertexConsumer vertices,
        Matrix4f position,
        Matrix3f normal,
        int light,
        float x,
        float y,
        float z,
        float u,
        float v,
        float normalZ
    ) {
        vertices.vertex(position, x, y, z)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal, 0.0F, 0.0F, normalZ)
            .next();
    }
}
