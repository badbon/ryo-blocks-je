package com.go4no.ryoblocks.client;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public final class KitaFeatherEffects {
    public static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1, 0.76F, 0.22F), 0.7F);
    public static final DustParticleEffect WHITE = new DustParticleEffect(new Vector3f(1, 0.97F, 0.85F), 0.55F);

    private KitaFeatherEffects() { }

    public static void impact(World world, Vec3d point, boolean charged) {
        float radius = charged ? 0.65F : 0.4F;
        // Four straight edges keep the brief impact outline square and pixel-like.
        for (int edge = 0; edge < 4; edge++) {
            for (int i = 0; i < 5; i++) {
                double a = -radius + 2 * radius * i / 4.0;
                double x = edge < 2 ? a : (edge == 2 ? -radius : radius);
                double z = edge < 2 ? (edge == 0 ? -radius : radius) : a;
                world.addParticle(GOLD, point.x + x, point.y + 0.15, point.z + z, x * 0.15, 0.03, z * 0.15);
            }
        }
        for (int i = 0; i < (charged ? 8 : 5); i++) {
            world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)),
                point.x, point.y + 0.15, point.z,
                (world.random.nextDouble() - 0.5) * 0.18, 0.06 + world.random.nextDouble() * 0.12,
                (world.random.nextDouble() - 0.5) * 0.18);
        }
        world.addParticle(ParticleTypes.FLASH, point.x, point.y + 0.1, point.z, 0, 0, 0);
    }
}
