package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;

public final class KitaAngelBossRenderer<T extends WitherEntity> extends MobEntityRenderer<T, KitaAngelBossModel<T>> {
    private static final float BOSS_SCALE = 1.85F;

    public KitaAngelBossRenderer(EntityRendererFactory.Context context) {
        super(
            context,
            new KitaAngelBossModel<>(context.getPart(KitaAngelBossModelLayers.KITA_ANGEL_BOSS)),
            1.15F
        );
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
        super.render(wither, entityYaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    public Identifier getTexture(T wither) {
        return KitaAngelBossAssets.PLAYER_TEXTURE;
    }
}
