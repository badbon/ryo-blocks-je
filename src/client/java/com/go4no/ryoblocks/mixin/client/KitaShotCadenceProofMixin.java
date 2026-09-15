package com.go4no.ryoblocks.mixin.client;

import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public abstract class KitaShotCadenceProofMixin {
    @Inject(method = "shootSkullAt(IDDDZ)V", at = @At("HEAD"))
    private void ryoBlocks$recordProofShot(int head, double x, double y, double z, boolean charged, CallbackInfo ci) {
        if (Boolean.getBoolean("ryoBlocks.bossCadenceProof")) {
            WitherEntity boss = (WitherEntity)(Object)this;
            org.slf4j.LoggerFactory.getLogger("KitaCadenceProof").info(
                "SHOT entity={} head={} age={} charged={} health={}", boss.getId(), head, boss.age, charged, boss.getHealth());
        }
    }
}
