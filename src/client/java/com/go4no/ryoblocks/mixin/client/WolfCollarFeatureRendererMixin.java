package com.go4no.ryoblocks.mixin.client;

import com.go4no.ryoblocks.RyoBlocks;
import net.minecraft.client.render.entity.feature.WolfCollarFeatureRenderer;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WolfCollarFeatureRenderer.class)
public abstract class WolfCollarFeatureRendererMixin {
    private static final Identifier BOCCHI_COLLAR_TEXTURE = new Identifier(
        RyoBlocks.MOD_ID,
        "textures/entity/wolf/bocchi_collar.png"
    );

    @ModifyArgs(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/feature/WolfCollarFeatureRenderer;renderModel("
                + "Lnet/minecraft/client/render/entity/model/EntityModel;"
                + "Lnet/minecraft/util/Identifier;"
                + "Lnet/minecraft/client/util/math/MatrixStack;"
                + "Lnet/minecraft/client/render/VertexConsumerProvider;"
                + "ILnet/minecraft/entity/LivingEntity;FFF)V"
        )
    )
    private void ryoBlocks$useBocchiDefaultCollar(Args args) {
        WolfEntity wolf = args.get(5);
        if (wolf.getCollarColor() != DyeColor.RED) {
            return;
        }

        args.set(1, BOCCHI_COLLAR_TEXTURE);
        args.set(6, 1.0F);
        args.set(7, 1.0F);
        args.set(8, 1.0F);
    }
}
