package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.RyoBlocks;
import com.go4no.ryoblocks.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in staging and observations only; attacks and obstacle escape use production AI. */
final class AngelBossVariantsProof {
    private static final Logger LOG = LoggerFactory.getLogger("AngelVariantsProof");
    private static final String[] NAMES = {"nijika", "kikuri", "nijika-combat", "kikuri-combat",
        "nijika-wall", "kikuri-wall", "kikuri-bedrock", "kikuri-griefing-off", "kita-control", "wither-control",
        "dorito-closeup", "potion-closeup"};
    private int stage = ProofRecording.ENABLED ? 2 : 0;
    private int tick;
    private int frames;
    private int passed;
    private volatile boolean observed;
    private volatile boolean result;
    private volatile boolean pictureSaved;
    private boolean pictureRequested;
    private final java.util.concurrent.atomic.AtomicInteger savedFrames = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger savedStyleViews = new java.util.concurrent.atomic.AtomicInteger();
    private WitherEntity boss;
    private LivingEntity target;
    private final java.util.Set<Integer> shots = new java.util.HashSet<>();
    private final java.util.Set<Integer> clearing = new java.util.HashSet<>();
    private final java.util.Set<String> potionTypes = new java.util.HashSet<>();
    private boolean wrongShot;
    private boolean potionHit;
    private boolean eggVerified;

    void tick(MinecraftClient client) {
        if (stage == NAMES.length || (ProofRecording.ENABLED && stage == 4)) {
            if (ProofRecording.ENABLED) LOG.info("COMBAT RECORDING CHECKS {}/2 frames={}/{}", passed, savedFrames.get(), frames);
            else LOG.info("VARIANT CHECKS {}/{} frames={}/{} styleViews={}/6", passed, NAMES.length, savedFrames.get(), frames, savedStyleViews.get());
            client.scheduleStop();
            return;
        }
        client.options.getFov().setValue(50);
        client.options.hudHidden = stage >= 2;
        client.getToastManager().clear();
        client.inGameHud.getChatHud().clear(false);
        client.getTutorialManager().setStep(net.minecraft.client.tutorial.TutorialStep.NONE);
        if (stage < 2) {
            Vec3d camera = tick < 80 ? new Vec3d(0.5, 182, -6)
                : tick < 110 ? new Vec3d(-7, 182, -3)
                : tick < 140 ? new Vec3d(-10, 182, 4.5) : new Vec3d(0.5, 182, 15);
            look(client, camera, new Vec3d(0.5, 182, 4.5));
        }
        else if (stage >= 10) look(client, new Vec3d(0.5, 182, 1.5), new Vec3d(0.5, 183, 4.5));
        else look(client, new Vec3d(-7, 187, -10), new Vec3d(5, 183, 4.5));
        int current = stage;
        int local = tick++;
        int duration = stage < 2 ? 180 : stage >= 10 ? 90 : 320;
        if (local < duration) client.getServer().execute(() -> observe(client.getServer(), current, local, duration));
        if (stage >= 2 && stage <= 5 && local >= 20 && local < 260) {
            ProofRecording.frame(stage, frames);
            ScreenshotRecorder.saveScreenshot(client.runDirectory, String.format(java.util.Locale.ROOT,
                ProofRecording.ENABLED ? "angel-recording-%04d.png" : "angel-variants-%04d.png", frames++), client.getFramebuffer(), message -> savedFrames.incrementAndGet());
        }
        if (local == 260 && stage >= 2 && stage <= 3) ProofRecording.frame(stage, -1);
        if (!pictureRequested && local == (stage < 2 || stage >= 10 ? 60 : 150)) {
            pictureRequested = true;
            ScreenshotRecorder.saveScreenshot(client.runDirectory, "angel-variant-" + NAMES[stage] + ".png",
                client.getFramebuffer(), message -> pictureSaved = true);
        }
        if (stage < 2 && (local == 100 || local == 130 || local == 160)) {
            String angle = local == 100 ? "three-quarter" : local == 130 ? "side" : "back";
            ScreenshotRecorder.saveScreenshot(client.runDirectory, "angel-variant-" + NAMES[stage] + "-" + angle + ".png",
                client.getFramebuffer(), message -> savedStyleViews.incrementAndGet());
        }
        if (local >= duration && observed && pictureSaved && savedFrames.get() == frames
            && (stage >= 2 || savedStyleViews.get() == (stage + 1) * 3)) {
            if (result) passed++;
            stage++;
            tick = 0;
            observed = false;
            pictureSaved = false;
            pictureRequested = false;
        }
        if (local > duration + 200) {
            LOG.error("VARIANT PROOF timed out in {}", NAMES[stage]);
            client.scheduleStop();
        }
    }

    private void look(MinecraftClient client, Vec3d camera, Vec3d at) {
        Vec3d d = at.subtract(camera.add(0, client.player.getStandingEyeHeight(), 0));
        client.player.updatePositionAndAngles(camera.x, camera.y, camera.z,
            (float)-Math.toDegrees(Math.atan2(d.x, d.z)), (float)-Math.toDegrees(Math.atan2(d.y, d.horizontalLength())));
        client.setCameraEntity(client.player);
    }

    private void command(MinecraftServer server, String text) {
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), text);
    }

    private void setup(MinecraftServer server, int trial) {
        var world = server.getOverworld();
        java.util.List<Entity> old = new java.util.ArrayList<>();
        for (var entity : world.iterateEntities()) {
            if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity)) old.add(entity);
        }
        old.forEach(Entity::discard);
        command(server, "gamerule doMobSpawning false");
        command(server, "gamerule mobGriefing " + (trial != 7));
        command(server, "fill -16 180 -12 16 195 20 air");
        command(server, "fill -16 179 -12 16 179 20 sea_lantern");
        command(server, "difficulty normal");
        SpawnEggItem egg = trial == 0 || trial == 2 || trial == 4 || trial == 10 ? RyoBlocks.NIJIKA_ANGEL_BOSS_SPAWN_EGG
            : trial == 8 ? RyoBlocks.KITA_ANGEL_BOSS_SPAWN_EGG : RyoBlocks.KIKURI_ANGEL_BOSS_SPAWN_EGG;
        var player = server.getPlayerManager().getPlayerList().get(0);
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(egg));
        egg.useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND,
            new BlockHitResult(new Vec3d(0.5, 180, 4.5), Direction.UP, new BlockPos(0, 179, 4), false)));
        for (var entity : world.iterateEntities()) if (entity instanceof AngelBossEntity angel) boss = angel;
        var expectedType = egg == RyoBlocks.NIJIKA_ANGEL_BOSS_SPAWN_EGG ? RyoBlocks.NIJIKA_ANGEL_BOSS
            : egg == RyoBlocks.KIKURI_ANGEL_BOSS_SPAWN_EGG ? RyoBlocks.KIKURI_ANGEL_BOSS : RyoBlocks.KITA_ANGEL_BOSS;
        eggVerified = boss != null && boss.getType() == expectedType && boss.getMaxHealth() == 300 && boss.getInvulnerableTimer() == 0;
        var nbt = new net.minecraft.nbt.NbtCompound();
        boss.saveNbt(nbt);
        var restored = EntityType.getEntityFromNbt(nbt, world);
        eggVerified &= restored.isPresent() && restored.get().getType() == expectedType;
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        if (trial == 9) {
            boss.discard();
            boss = EntityType.WITHER.create(world);
            boss.refreshPositionAndAngles(0.5, 180, 4.5, 180, 0);
            world.spawnEntity(boss);
        }
        boss.setYaw(180);
        boss.setBodyYaw(180);
        boss.setHeadYaw(180);
        shots.clear();
        clearing.clear();
        potionTypes.clear();
        wrongShot = false;
        potionHit = false;
        if (trial < 2 || trial >= 10) boss.setAiDisabled(true);
        if (trial >= 4 && trial <= 7) command(server, "fill 3 180 -1 7 189 10 " + (trial == 6 ? "bedrock" : "stone"));
        target = null;
        if (trial >= 2 && trial < 10) {
            target = EntityType.IRON_GOLEM.create(world);
            target.refreshPositionAndAngles(trial >= 4 && trial <= 7 ? 12 : 8, 180, 4.5, 180, 0);
            ((MobEntity)target).setAiDisabled(true);
            target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(5000);
            target.setHealth(5000);
            world.spawnEntity(target);
            boss.setTarget(target);
        }
        if (trial >= 10) {
            boss.setPosition(-5, 180, 4.5);
            ProjectileEntity showcase;
            if (trial == 10) showcase = new WitherSkullEntity(world, boss, 0, 0, 0);
            else {
                var bottle = new PotionEntity(world, boss);
                bottle.setItem(net.minecraft.potion.PotionUtil.setPotion(new ItemStack(net.minecraft.item.Items.SPLASH_POTION),
                    net.minecraft.potion.Potions.HARMING));
                showcase = bottle;
            }
            showcase.setPosition(0.5, 183, 4.5);
            showcase.setNoGravity(true);
            world.spawnEntity(showcase);
        }
        LOG.info("VARIANT START {} egg={} type={}", NAMES[trial], eggVerified, boss.getType());
    }

    private void observe(MinecraftServer server, int trial, int local, int duration) {
        if (local == 0) setup(server, trial);
        var world = server.getOverworld();
        for (var entity : world.iterateEntities()) {
            if (entity instanceof ProjectileEntity projectile && projectile.getOwner() == boss) {
                shots.add(projectile.getId());
                if (projectile.getCommandTags().contains(AngelBossEntity.OBSTACLE_SHOT_TAG)) clearing.add(projectile.getId());
                if (boss instanceof KikuriAngelBossEntity) {
                    if (!(projectile instanceof PotionEntity)) wrongShot = true;
                    else potionTypes.add(net.minecraft.registry.Registries.POTION.getId(
                        net.minecraft.potion.PotionUtil.getPotion(((PotionEntity)projectile).getStack())).toString());
                } else if (!(projectile instanceof WitherSkullEntity)) wrongShot = true;
            }
        }
        if (target != null && (target.hasStatusEffect(StatusEffects.POISON) || target.hasStatusEffect(StatusEffects.SLOWNESS)
            || target.hasStatusEffect(StatusEffects.WEAKNESS))) potionHit = true;
        if (local == duration - 1) {
            boolean escaped = boss.getX() > 8;
            boolean hit = target != null && (target.getHealth() < 5000 || potionHit);
            result = eggVerified && !wrongShot && switch (trial) {
                case 0, 1 -> boss.isAlive();
                case 4, 5 -> escaped && clearing.size() >= 6;
                case 6, 7 -> !escaped && clearing.isEmpty();
                case 10, 11 -> shots.size() == 1;
                default -> shots.size() >= 6 && hit && (trial != 3 || potionHit);
            };
            LOG.info("VARIANT RESULT {} PASS={} shots={} clearing={} hit={} potionHit={} potions={} pos={}",
                NAMES[trial], result, shots.size(), clearing.size(), hit, potionHit, potionTypes, boss.getPos());
            observed = true;
        }
    }
}
