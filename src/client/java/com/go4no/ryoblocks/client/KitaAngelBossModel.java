package com.go4no.ryoblocks.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.MathHelper;

public final class KitaAngelBossModel<T extends WitherEntity> extends PlayerEntityModel<T> {
    private float hoverOffset;

    public KitaAngelBossModel(ModelPart root, boolean slimArms) {
        super(root, slimArms);
    }

    public float getHoverOffset() {
        return this.hoverOffset;
    }

    @Override
    public void setAngles(T boss, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(boss, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        this.hoverOffset = MathHelper.sin(animationProgress * 0.08F) * 0.6F;
        this.rightArm.pitch = 0.46F;
        this.rightArm.roll = 0.32F;
        this.leftArm.pitch = 0.46F;
        this.leftArm.roll = -0.32F;
        this.rightSleeve.copyTransform(this.rightArm);
        this.leftSleeve.copyTransform(this.leftArm);
        float legSway = MathHelper.sin(animationProgress * 0.06F) * 0.08F;
        this.rightLeg.pitch = -legSway;
        this.leftLeg.pitch = legSway;
        this.rightPants.copyTransform(this.rightLeg);
        this.leftPants.copyTransform(this.leftLeg);
    }
}
