package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class KitaAngelBossRenderer<T extends WitherEntity> extends MobEntityRenderer<T, KitaAngelBossModel<T>> {
    private static final float BOSS_SCALE = 1.85F;
    private final Identifier texture;

    public KitaAngelBossRenderer(EntityRendererFactory.Context context) {
        this(context, KitaAngelBossAssets.PLAYER_TEXTURE, KitaAngelBossAssets.SLIM_ARMS);
    }

    public KitaAngelBossRenderer(EntityRendererFactory.Context context, Identifier texture, boolean slimArms) {
        super(
            context,
            new KitaAngelBossModel<>(context.getPart(slimArms ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slimArms),
            1.15F
        );
        this.texture = texture;
        this.addFeature(new KitaAngelHaloFeatureRenderer<>(this));
        this.addFeature(new KitaAngelWingsFeatureRenderer<>(this));
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
        float hover = -3.0F + MathHelper.sin((wither.age + tickDelta) * 0.025F) * 0.6F;
        matrices.translate(0.0F, hover / 16.0F, 0.0F);
        super.render(wither, entityYaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    public Identifier getTexture(T wither) {
        return texture;
    }
}
