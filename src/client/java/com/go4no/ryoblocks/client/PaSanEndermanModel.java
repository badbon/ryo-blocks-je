package com.go4no.ryoblocks.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.mob.EndermanEntity;

/**
 * A normal slim player model that preserves the meaningful Enderman arm pose
 * while a block is carried. Combat motion otherwise stays with the vanilla
 * biped animation supplied by PlayerEntityModel.
 */
public final class PaSanEndermanModel extends PlayerEntityModel<EndermanEntity> {
    private boolean carryingBlock;

    public PaSanEndermanModel(ModelPart root, boolean slimArms) {
        super(root, slimArms);
    }

    public void setCarryingBlock(boolean carryingBlock) {
        this.carryingBlock = carryingBlock;
    }

    @Override
    public void setAngles(EndermanEntity enderman, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(enderman, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        if (this.carryingBlock) {
            // These are the native Enderman carrying rotations. Combined with
            // PaSanEndermanBlockFeatureRenderer's vanilla transform, the block
            // rests in the raised hands rather than intersecting the torso.
            this.rightArm.pitch = -0.5F;
            this.leftArm.pitch = -0.5F;
            this.rightArm.roll = 0.05F;
            this.leftArm.roll = -0.05F;
        }
    }
}
