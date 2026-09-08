package com.go4no.ryoblocks.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.MinecraftServer;
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
    private static final int CAPTURE_DELAY_TICKS = 180;

    private boolean staged;
    private boolean screenshotRequested;
    private volatile boolean screenshotSaved;
    private int readyTicks;
    private int savedTicks;

    @Override
    public void onEndTick(MinecraftClient client) {
        MinecraftServer server = client.getServer();
        if (client.world == null || client.player == null || server == null) {
            return;
        }

        if (!staged) {
            staged = true;
            String playerName = client.player.getGameProfile().getName();
            server.execute(() -> {
                var commands = server.getCommandManager();
                var source = server.getCommandSource();
                commands.executeWithPrefix(source, "gamemode creative " + playerName);
                commands.executeWithPrefix(source, "time set noon");
                commands.executeWithPrefix(source, "weather clear");
                commands.executeWithPrefix(source, "kill @e[type=minecraft:wither]");
                commands.executeWithPrefix(source, "fill -12 176 -10 12 190 14 minecraft:air");
                commands.executeWithPrefix(source, "fill -8 179 -6 8 179 10 minecraft:smooth_quartz");
                commands.executeWithPrefix(source, "summon minecraft:wither 0 182 4 {NoAI:1b,Silent:1b,Invulnerable:1b,Invul:0}");
                commands.executeWithPrefix(source, "tp " + playerName + " 0 182 -8 0 12");
            });
            client.getWindow().setWindowedSize(1280, 720);
            client.options.tutorialStep = net.minecraft.client.tutorial.TutorialStep.NONE;
            client.options.hudHidden = true;
            return;
        }

        client.player.updatePositionAndAngles(0.5, 182.0, -8.0, 0.0F, 12.0F);
        client.setCameraEntity(client.player);
        readyTicks++;
        if (!screenshotRequested && readyTicks >= CAPTURE_DELAY_TICKS) {
            screenshotRequested = true;
            String filename = "kita-angel-boss-proof-" + FILE_TIME.format(LocalDateTime.now()) + ".png";
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

        if (screenshotSaved && ++savedTicks >= 20) {
            client.scheduleStop();
        }
    }
}
