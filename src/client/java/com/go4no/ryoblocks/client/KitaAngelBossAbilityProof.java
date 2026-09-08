package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.RyoBlocks;
import com.go4no.ryoblocks.entity.KitaAngelBossEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stages observations only; combat, projectiles, damage, regeneration and drops use vanilla logic. */
final class KitaAngelBossAbilityProof {
    private static final Logger LOG = LoggerFactory.getLogger("KitaAbilityProof");
    private static final String[] NAMES = {
        "01-egg-spawn", "02-live-skulls", "03-wither-impact", "04-half-health",
        "05-healing-before", "06-healing-after", "07-blocks-before", "08-blocks-after",
        "09-death-drops", "10-forced-charge", "11-forced-explosion", "12-idle-blue-skull",
        "13-fire-vulnerability", "14-wither-rose"
    };
    private int stage = Integer.getInteger("ryoBlocks.bossAbilityScene", 0);
    private int ticks;
    private boolean requested;
    private volatile boolean saved;
    private volatile boolean ready;
    private volatile String evidence = "";
    private volatile Vec3d focus = new Vec3d(0.5, 182, 4.5);
    private KitaAngelBossEntity boss;
    private LivingEntity target;
    private int explosionFrames;
    private int victimDeathTicks;
    private boolean victimWeakened;

    void tick(MinecraftClient client) {
        if (stage >= NAMES.length) {
            client.scheduleStop();
            return;
        }
        client.options.getFov().setValue(50);
        client.getTutorialManager().setStep(net.minecraft.client.tutorial.TutorialStep.NONE);
        client.getToastManager().clear();
        client.inGameHud.getChatHud().clear(false);
        Vec3d at = focus;
        double distance = stage == 8 || stage == 13 ? 0.4 : 1;
        client.player.updatePositionAndAngles(at.x - 10 * distance, at.y + 4 * distance,
            at.z - 13 * distance, -37.57F, stage == 8 || stage == 13 ? 27 : 15);
        client.setCameraEntity(client.player);
        if (saved) {
            stage++;
            ticks = 0;
            requested = false;
            saved = false;
            ready = false;
            return;
        }
        int currentStage = stage;
        int currentTicks = ++ticks;
        client.getServer().execute(() -> observe(client.getServer(), currentStage, currentTicks));
        if (!requested && ready) {
            requested = true;
            LOG.info("CAPTURE {}: {}", NAMES[stage], evidence);
            ScreenshotRecorder.saveScreenshot(client.runDirectory, "kita-abilities-" + NAMES[stage] + ".png",
                client.getFramebuffer(), message -> saved = true);
        }
        if (ticks > 700) {
            LOG.error("Missing observation for {}", NAMES[stage]);
            client.scheduleStop();
        }
    }

    private void command(MinecraftServer server, String command) {
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
    }

    private void reset(MinecraftServer server) {
        // Remove previous staging entities without manufacturing death effects or rewards.
        java.util.List<Entity> previous = new java.util.ArrayList<>();
        for (Entity entity : server.getOverworld().iterateEntities()) {
            if (entity instanceof KitaAngelBossEntity || entity instanceof WitherSkullEntity
                || entity.getType() == EntityType.IRON_GOLEM || entity.getType() == EntityType.SHEEP
                || entity.getType() == EntityType.ITEM || entity.getType() == EntityType.EXPERIENCE_ORB) {
                previous.add(entity);
            }
        }
        previous.forEach(Entity::discard);
        command(server, "fill -16 180 -12 16 195 20 air");
        command(server, "fill -16 179 -12 16 179 20 stone");
        command(server, "gamerule doMobSpawning false");
        command(server, "gamerule mobGriefing true");
        command(server, "difficulty normal");
        ServerWorld world = server.getOverworld();
        var player = server.getPlayerManager().getPlayerList().get(0);
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(RyoBlocks.KITA_ANGEL_BOSS_SPAWN_EGG));
        RyoBlocks.KITA_ANGEL_BOSS_SPAWN_EGG.useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND,
            new BlockHitResult(new Vec3d(0.5, 180, 4.5), Direction.UP, new BlockPos(0, 179, 4), false)));
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof KitaAngelBossEntity candidate && candidate.isAlive()) {
                boss = candidate;
                break;
            }
        }
        target = null;
        focus = boss.getPos().add(0, 1.5, 0);
    }

    private void observe(MinecraftServer server, int scene, int tick) {
        ServerWorld world = server.getOverworld();
        if (tick == 1) {
            switch (scene) {
                case 0, 4, 6, 9, 11, 12, 13 -> reset(server);
                default -> { }
            }
            switch (scene) {
                case 1 -> {
                    target = EntityType.IRON_GOLEM.create(world);
                    target.refreshPositionAndAngles(5, 180, 7, 180, 0);
                    ((net.minecraft.entity.mob.MobEntity)target).setAiDisabled(true);
                    target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);
                    target.setHealth(1000);
                    world.spawnEntity(target);
                }
                case 3 -> boss.setHealth(120);
                case 4 -> boss.setHealth(210);
                case 6 -> {
                    command(server, "fill -1 180 3 1 183 5 stone");
                    command(server, "fill 0 180 4 0 183 4 air");
                }
                case 8 -> {
                    java.util.List<Entity> debris = new java.util.ArrayList<>();
                    for (Entity entity : world.iterateEntities()) {
                        if (entity instanceof net.minecraft.entity.ItemEntity) debris.add(entity);
                    }
                    debris.forEach(Entity::discard);
                    focus = boss.getPos().add(0, 0.5, 0);
                    boss.damage(world.getDamageSources().playerAttack(server.getPlayerManager().getPlayerList().get(0)), 10000);
                }
                case 9 -> boss.onSummoned();
                case 12 -> boss.setOnFireFor(5);
                case 13 -> {
                    target = EntityType.SHEEP.create(world);
                    target.refreshPositionAndAngles(4, 180, 7, 180, 0);
                    ((net.minecraft.entity.mob.MobEntity)target).setAiDisabled(true);
                    target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(80);
                    target.setHealth(80);
                    world.spawnEntity(target);
                }
                default -> { }
            }
        }
        if (scene == 13 && target != null) {
            focus = target.getPos();
        } else if (scene != 8 && boss.isAlive()) {
            focus = target != null && target.isAlive()
                ? boss.getPos().add(target.getPos()).multiply(0.5).add(0, 1, 0)
                : boss.getPos().add(0, 1.5, 0);
        }
        if (scene == 7 && tick == 1) {
            boss.damage(world.getDamageSources().playerAttack(server.getPlayerManager().getPlayerList().get(0)), 1);
        }
        boolean skull = false;
        boolean blue = false;
        int drops = 0;
        int roses = 0;
        if (scene == 13 && !victimWeakened && target != null && target.isAlive()
            && target.hasStatusEffect(StatusEffects.WITHER)) {
            // Let the applied Wither effect finish the victim after the impact blast has ended.
            target.setHealth(1);
            boss.setAiDisabled(true);
            victimWeakened = true;
        }
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof WitherSkullEntity projectile && projectile.getOwner() == boss) {
                skull = true;
                if (projectile.isCharged()) blue = true;
            }
            if (entity instanceof net.minecraft.entity.ItemEntity item
                && item.getStack().isOf(net.minecraft.item.Items.NETHER_STAR)) drops++;
            if (entity instanceof net.minecraft.entity.ItemEntity item
                && item.getStack().isOf(net.minecraft.item.Items.WITHER_ROSE)) roses++;
        }
        evidence = "health=" + boss.getHealth() + ", invul=" + boss.getInvulnerableTimer()
            + ", skull=" + skull + ", charged=" + blue + ", netherStars=" + drops
            + ", targetWither=" + (target != null && target.hasStatusEffect(StatusEffects.WITHER))
            + ", fireImmune=" + boss.isFireImmune() + ", roseItems=" + roses;
        ready = switch (scene) {
            case 0, 3 -> tick >= 35;
            case 1 -> tick >= 20 && skull;
            case 2 -> target != null && target.hasStatusEffect(StatusEffects.WITHER);
            case 4 -> tick >= 5;
            case 5 -> tick >= 100;
            case 6 -> tick >= 5;
            case 7 -> tick >= 25;
            case 8 -> tick >= 28 && drops > 0;
            case 9 -> tick >= 90;
            case 10 -> boss.getInvulnerableTimer() == 0 && ++explosionFrames >= 4;
            case 11 -> blue;
            case 12 -> tick >= 30;
            case 13 -> target != null && !target.isAlive() && ++victimDeathTicks >= 65;
            default -> false;
        };
    }
}
