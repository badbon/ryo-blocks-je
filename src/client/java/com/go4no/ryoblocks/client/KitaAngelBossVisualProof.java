package com.go4no.ryoblocks.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hidden, opt-in framebuffer proof for the Kita Angel boss renderer.
 * It stages only the disposable world supplied to runClient.
 */
public final class KitaAngelBossVisualProof implements ClientTickEvents.EndTick {
    private static final Logger LOGGER = LoggerFactory.getLogger("RyoBlocks/KitaAngelBossVisualProof");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String PROOF_WORLD = "KitaAngelBossProof";
    private static final int CAPTURE_DELAY_TICKS = 120;
    private static final int CAPTURE_SETTLE_TICKS = 35;

    private boolean worldStartRequested;
    private boolean staged;
    private boolean screenshotRequested;
    private volatile boolean screenshotSaved;
    private int captureIndex;
    private int readyTicks;
    private int savedTicks;

    @Override
    public void onEndTick(MinecraftClient client) {
        if (client.world == null && !worldStartRequested) {
            worldStartRequested = true;
            LOGGER.info("Creating disposable boss proof world {}", PROOF_WORLD);
            client.createIntegratedServerLoader().createAndStart(
                PROOF_WORLD,
                new LevelInfo(
                    PROOF_WORLD,
                    GameMode.CREATIVE,
                    false,
                    Difficulty.NORMAL,
                    true,
                    new GameRules(),
                    DataConfiguration.SAFE_MODE
                ),
                new GeneratorOptions(0L, false, false),
                WorldPresets::createDemoOptions
            );
            return;
        }

        MinecraftServer server = client.getServer();
        if (client.world == null || client.player == null || server == null) {
            return;
        }

        client.setScreen(null);
        client.setOverlay(null);

        if (!staged) {
            staged = true;
            String playerName = client.player.getGameProfile().getName();
            server.execute(() -> {
                var commands = server.getCommandManager();
                var source = server.getCommandSource();
                commands.executeWithPrefix(source, "gamemode creative " + playerName);
                commands.executeWithPrefix(source, "gamerule sendCommandFeedback false");
                commands.executeWithPrefix(source, "gamerule commandBlockOutput false");
                commands.executeWithPrefix(source, "time set noon");
                commands.executeWithPrefix(source, "weather clear");
                commands.executeWithPrefix(source, "kill @e[type=minecraft:wither]");
                commands.executeWithPrefix(source, "kill @e[type=ryo-blocks:kita_angel_boss]");
                commands.executeWithPrefix(source, "fill -12 176 -10 12 190 14 minecraft:air");
                commands.executeWithPrefix(source, "fill -10 179 -6 10 179 10 minecraft:smooth_quartz");
                commands.executeWithPrefix(source, "give " + playerName + " ryo-blocks:kita_angel_boss_spawn_egg");
                commands.executeWithPrefix(source, "summon ryo-blocks:kita_angel_boss 0 182 4 {NoAI:1b,Silent:1b,Invulnerable:1b,Invul:0,Rotation:[180.0f,0.0f],CustomName:'{\"text\":\"Kita Angel Boss\",\"color\":\"light_purple\"}'}");
                commands.executeWithPrefix(source, "summon minecraft:wither 5 182 5 {NoAI:1b,Silent:1b,Invulnerable:1b,Invul:0,Rotation:[180.0f,0.0f],CustomName:'{\"text\":\"Vanilla Wither\",\"color\":\"gray\"}'}");
                commands.executeWithPrefix(source, "tp " + playerName + " 0 182 -9 0 10");
            });
            client.getWindow().setWindowedSize(1280, 720);
            client.options.tutorialStep = net.minecraft.client.tutorial.TutorialStep.NONE;
            client.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            client.options.hudHidden = false;
            return;
        }

        placeCamera(client);
        client.setCameraEntity(client.player);
        client.inGameHud.getChatHud().clear(false);
        readyTicks++;
        if (!screenshotRequested && readyTicks >= CAPTURE_DELAY_TICKS) {
            screenshotRequested = true;
            String filename = "kita-angel-boss-proof-" + captureName(captureIndex) + "-" + FILE_TIME.format(LocalDateTime.now()) + ".png";
            ScreenshotRecorder.saveScreenshot(
                client.runDirectory,
                filename,
                client.getFramebuffer(),
                message -> {
                    LOGGER.info("{}", message.getString());
                    screenshotSaved = true;
                }
            );
            return;
        }

        if (screenshotSaved && ++savedTicks >= CAPTURE_SETTLE_TICKS) {
            if (captureIndex == 0) {
                server.execute(() -> server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "kill @e[type=minecraft:wither]"
                ));
            }
            captureIndex++;
            if (captureIndex < 3) {
                screenshotRequested = false;
                screenshotSaved = false;
                readyTicks = 0;
                savedTicks = 0;
                return;
            }
            client.scheduleStop();
        }
    }

    private static String captureName(int index) {
        return switch (index) {
            case 0 -> "front-entity-separation";
            case 1 -> "side-wing-attachment";
            case 2 -> "back-wing-attachment";
            default -> "unknown";
        };
    }

    private void placeCamera(MinecraftClient client) {
        switch (captureIndex) {
            case 1 -> client.player.updatePositionAndAngles(-10.0, 183.0, 4.5, -90.0F, 8.0F);
            case 2 -> client.player.updatePositionAndAngles(0.0, 183.0, 14.0, 180.0F, 8.0F);
            default -> client.player.updatePositionAndAngles(0.5, 182.0, -8.0, 0.0F, 10.0F);
        }
    }
}
