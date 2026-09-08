package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.BocchiBossAssets;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;

public final class BocchiBossRenderer<T extends WitherEntity> extends MobEntityRenderer<T, PlayerEntityModel<T>> {
    private static final float BOSS_SCALE = 2.4F;

    public BocchiBossRenderer(EntityRendererFactory.Context context) {
        super(
            context,
            new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), BocchiBossAssets.SLIM_ARMS),
            1.0F
        );
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
        return BocchiBossAssets.PLAYER_TEXTURE;
    }
}
