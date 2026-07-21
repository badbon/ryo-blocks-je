package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.PaSanAssets;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Renders both vanilla Endermen and the registered Pa-san Enderman subtype.
 */
public final class PaSanEndermanRenderer<T extends EndermanEntity> extends MobEntityRenderer<T, PaSanEndermanModel<T>> {
    private final Random random = Random.create();

    public PaSanEndermanRenderer(EntityRendererFactory.Context context) {
        super(
            context,
            new PaSanEndermanModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), PaSanAssets.SLIM_ARMS),
            0.5F
        );
        this.addFeature(new PaSanEndermanBlockFeatureRenderer<>(this, context.getBlockRenderManager()));
    }

    @Override
    public void render(
        T enderman,
        float entityYaw,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light
    ) {
        this.getModel().setCarryingBlock(enderman.getCarriedBlock() != null);
        this.getModel().setAngry(enderman.isAngry());
        super.render(enderman, entityYaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Vec3d getPositionOffset(T enderman, float tickDelta) {
        if (enderman.isAngry()) {
            return new Vec3d(this.random.nextGaussian() * 0.02D, 0.0D, this.random.nextGaussian() * 0.02D);
        }
        return super.getPositionOffset(enderman, tickDelta);
    }

    @Override
    public Identifier getTexture(T enderman) {
        return PaSanAssets.PLAYER_TEXTURE;
    }
}
