package com.go4no.ryoblocks.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.RotationAxis;

/** Renders the carried state with Minecraft's native Enderman block transform. */
public final class PaSanEndermanBlockFeatureRenderer<T extends EndermanEntity> extends FeatureRenderer<T, PaSanEndermanModel<T>> {
    private final BlockRenderManager blockRenderManager;

    public PaSanEndermanBlockFeatureRenderer(
        FeatureRendererContext<T, PaSanEndermanModel<T>> context,
        BlockRenderManager blockRenderManager
    ) {
        super(context);
        this.blockRenderManager = blockRenderManager;
    }

    @Override
    public void render(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        T enderman,
        float limbAngle,
        float limbDistance,
        float tickDelta,
        float animationProgress,
        float headYaw,
        float headPitch
    ) {
        BlockState carriedBlock = enderman.getCarriedBlock();
        if (carriedBlock == null) {
            return;
        }

        matrices.push();
        matrices.translate(0.0F, 0.6875F, -0.75F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
        matrices.translate(0.25F, 0.1875F, 0.25F);
        matrices.scale(-0.5F, -0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        this.blockRenderManager.renderBlockAsEntity(
            carriedBlock,
            matrices,
            vertexConsumers,
            light,
            OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
