package com.go4no.ryoblocks.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.MathHelper;

/**
 * A slim player rig driven by the vanilla Enderman animation rules.
 */
public final class PaSanEndermanModel<T extends EndermanEntity> extends PlayerEntityModel<T> {
    private boolean carryingBlock;
    private boolean angry;

    public PaSanEndermanModel(ModelPart root, boolean slimArms) {
        super(root, slimArms);
    }

    public void setCarryingBlock(boolean carryingBlock) {
        this.carryingBlock = carryingBlock;
    }

    public void setAngry(boolean angry) {
        this.angry = angry;
    }

    @Override
    public void setAngles(T enderman, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(enderman, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        // Keep the normal player silhouette, but match EndermanEntityModel's
        // deliberately restrained walk cycle and limb limits exactly.
        this.rightArm.pitch = MathHelper.clamp(this.rightArm.pitch * 0.5F, -0.4F, 0.4F);
        this.leftArm.pitch = MathHelper.clamp(this.leftArm.pitch * 0.5F, -0.4F, 0.4F);
        this.rightLeg.pitch = MathHelper.clamp(this.rightLeg.pitch * 0.5F, -0.4F, 0.4F);
        this.leftLeg.pitch = MathHelper.clamp(this.leftLeg.pitch * 0.5F, -0.4F, 0.4F);

        if (this.carryingBlock) {
            // These are the native Enderman carrying rotations. Combined with
            // PaSanEndermanBlockFeatureRenderer's vanilla transform, the block
            // rests in the raised hands rather than intersecting the torso.
            this.rightArm.pitch = -0.5F;
            this.leftArm.pitch = -0.5F;
            this.rightArm.roll = 0.05F;
            this.leftArm.roll = -0.05F;
        }

        if (this.angry) {
            // The vanilla Enderman raises its head when provoked. A smaller
            // offset preserves Pa-san's connected player-model neckline.
            this.head.pivotY -= 1.0F;
            this.hat.pivotY = this.head.pivotY;
        }
    }
}
