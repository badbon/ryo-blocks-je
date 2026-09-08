package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.KitaAngelBossAssets;
import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.WitherSkullEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The underlying skull retains its physics and damage; only Kita's shots use this model. */
public final class KitaFeatherProjectileRenderer extends WitherSkullEntityRenderer {
    private static final Identifier IVORY = new Identifier("minecraft", "textures/block/snow.png");
    private final ModelPart vane;
    private final ModelPart shaft;
    private final ModelPart halo;

    public KitaFeatherProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
        ModelData data = new ModelData();
        var barbs = data.getRoot().addChild("vane", ModelPartBuilder.create(), ModelTransform.NONE);
        float[] widths = {2, 3.5F, 4.5F, 4, 3, 1.5F};
        for (int i = 0; i < widths.length; i++) {
            float z = -7 + i * 2.5F;
            float width = widths[i];
            barbs.addChild("right" + i, ModelPartBuilder.create()
                .uv(0, 0).cuboid(0.3F, -0.7F, 0, width, 1.4F, 2.1F),
                ModelTransform.of(0, 0, z, 0, 0.4F, 0));
            barbs.addChild("left" + i, ModelPartBuilder.create()
                .uv(0, 0).cuboid(-width - 0.3F, -0.7F, 0, width, 1.4F, 2.1F),
                ModelTransform.of(0, 0, z - 0.7F, 0, -0.4F, 0));
        }
        data.getRoot().addChild("shaft", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-0.5F, -0.8F, -11, 1, 1.6F, 21)
            .uv(0, 0).cuboid(-0.3F, -0.7F, 10, 0.6F, 1.4F, 2), ModelTransform.NONE);
        data.getRoot().addChild("halo", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-3.5F, -3.5F, -8, 7, 0.5F, 0.5F)
            .uv(0, 0).cuboid(-3.5F, 3, -8, 7, 0.5F, 0.5F)
            .uv(0, 0).cuboid(-3.5F, -3, -8, 0.5F, 6, 0.5F)
            .uv(0, 0).cuboid(3, -3, -8, 0.5F, 6, 0.5F), ModelTransform.NONE);
        ModelPart root = TexturedModelData.of(data, 16, 16).createModel();
        vane = root.getChild("vane");
        shaft = root.getChild("shaft");
        halo = root.getChild("halo");
    }

    @Override
    public void render(WitherSkullEntity projectile, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        if (!(projectile.getOwner() instanceof KitaAngelBossEntity)) {
            super.render(projectile, yaw, tickDelta, matrices, consumers, light);
            return;
        }
        matrices.push();
        Vec3d velocity = projectile.getVelocity();
        if (velocity.lengthSquared() < 0.00001) {
            velocity = new Vec3d(projectile.powerX, projectile.powerY, projectile.powerZ);
        }
        if (velocity.lengthSquared() > 0.00001) {
            matrices.multiply(new Quaternionf().rotationTo(new Vector3f(0, 0, 1),
                new Vector3f((float)velocity.x, (float)velocity.y, (float)velocity.z).normalize()));
        }
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(35));
        float scale = projectile.isCharged() ? 1.35F : 1;
        matrices.scale(scale, scale, scale);
        vane.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(IVORY)),
            light, OverlayTexture.DEFAULT_UV);
        shaft.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(KitaAngelBossAssets.HALO_TEXTURE)),
            light, OverlayTexture.DEFAULT_UV);
        if (projectile.isCharged()) {
            halo.roll = (projectile.age + tickDelta) * 0.08F;
            halo.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(KitaAngelBossAssets.HALO_TEXTURE)),
                light, OverlayTexture.DEFAULT_UV);
        }
        matrices.pop();
    }
}
