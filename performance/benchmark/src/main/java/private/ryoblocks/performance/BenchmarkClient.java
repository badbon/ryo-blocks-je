package com.go4no.ryoperformance;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

/** Fixed monotonic-clock scene controller; tick count never decides the measurement length. */
public final class BenchmarkClient implements ClientModInitializer {
    private static boolean staged, measuring; private static long warmupStart;
    private static final long WARMUP = seconds("ryoPerformance.warmupSeconds", 20), MEASURE = seconds("ryoPerformance.measurementSeconds", 60);
    private static long seconds(String k, long d) { return Math.max(1, Long.getLong(k,d)) * 1_000_000_000L; }
    @Override public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(BenchmarkClient::tick); }
    private static void tick(MinecraftClient client) {
        MinecraftServer server=client.getServer(); if(client.world==null||client.player==null||server==null)return;
        if(!staged) { staged=true; String p=client.player.getGameProfile().getName(); server.execute(()->{var c=server.getCommandManager();var s=server.getCommandSource(); c.executeWithPrefix(s,"gamemode creative "+p);c.executeWithPrefix(s,"time set noon");c.executeWithPrefix(s,"weather clear");c.executeWithPrefix(s,"fill -16 176 -16 16 192 16 minecraft:air");c.executeWithPrefix(s,"fill -12 178 -4 12 178 20 minecraft:stone");c.executeWithPrefix(s,"fill -4 179 5 4 179 13 minecraft:lava");c.executeWithPrefix(s,"fill 7 179 7 7 187 7 minecraft:stone");c.executeWithPrefix(s,"setblock 6 187 7 minecraft:lava");c.executeWithPrefix(s,"tp "+p+" 0 182 -8 0 20");}); client.getWindow().setWindowedSize(1280,720);client.options.getViewDistance().setValue(12);client.options.getSimulationDistance().setValue(12);client.options.getFov().setValue(70);client.options.getEnableVsync().setValue(false);client.options.getMaxFps().setValue(10000);client.options.hudHidden=true;warmupStart=System.nanoTime();return; }
        client.player.updatePositionAndAngles(.5,182,-7.5,0,20); client.setCameraEntity(client.player); long now=System.nanoTime();
        if(!measuring && now-warmupStart>=WARMUP){ BenchmarkRecorder.verifyRuntimeContract(client); measuring=true; BenchmarkRecorder.beginMeasurement(); }
        if(measuring && now-warmupStart>=WARMUP+MEASURE) BenchmarkRecorder.finishAndStop(client);
    }
}
