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
 * Hidden, opt-in framebuffer proof for player-visible terrain work.
 * It never runs in ordinary play and only mutates the disposable world supplied to runClient.
 */
public final class KitaLavaVisualProof implements ClientTickEvents.EndTick {
    private static final Logger LOGGER = LoggerFactory.getLogger("RyoBlocks/KitaLavaVisualProof");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int CAPTURE_DELAY_TICKS = 240;

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
                commands.executeWithPrefix(source, "fill -12 176 -6 12 190 14 minecraft:air");
                commands.executeWithPrefix(source, "fill -8 180 -2 8 180 11 minecraft:stone");
                commands.executeWithPrefix(source, "fill -3 181 2 3 181 8 minecraft:lava");
                commands.executeWithPrefix(source, "fill 6 181 4 6 188 8 minecraft:stone");
                commands.executeWithPrefix(source, "setblock 5 188 6 minecraft:lava");
                commands.executeWithPrefix(source, "tp " + playerName + " 0 185 -8 0 25");
            });
            client.getWindow().setWindowedSize(1280, 720);
            client.options.tutorialStep = net.minecraft.client.tutorial.TutorialStep.NONE;
            client.options.hudHidden = true;
            return;
        }

        client.player.updatePositionAndAngles(0.5, 185.0, -7.5, 0.0F, 25.0F);
        client.setCameraEntity(client.player);
        readyTicks++;
        if (!screenshotRequested && readyTicks >= CAPTURE_DELAY_TICKS) {
            screenshotRequested = true;
            String filename = "kita-lava-proof-" + FILE_TIME.format(LocalDateTime.now()) + ".png";
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
