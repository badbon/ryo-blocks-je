package com.go4no.ryoperformance;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import com.go4no.ryoperformance.mixin.MinecraftClientAccessor;
import org.lwjgl.opengl.GL33;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Benchmark-owned submitted-frame recorder. It never claims that a hidden swap was presented. */
public final class BenchmarkRecorder {
    private static final int RING = 8;
    private static final int[] queries = new int[RING];
    private static final boolean[] issued = new boolean[RING];
    private static final ArrayList<Sample> samples = new ArrayList<>();
    private static final ArrayList<Long> swaps = new ArrayList<>();
    private static boolean enabled, gpuSupported, queryOpen, activeFrame;
    private static int openSlot = -1, gpuValid, gpuDropped;
    private static long cpuStart;
    private static int framebufferWidth, framebufferHeight, fov, renderDistance, simulationDistance, fpsCap;
    private static boolean vsync;
    private record Sample(long cpuNanos, long gpuNanos) {}
    private BenchmarkRecorder() {}

    public static void beforeRender() {
        activeFrame = enabled;
        if (!activeFrame) return;
        cpuStart = System.nanoTime();
        if (!gpuSupported) return;
        collectAvailableQueries();
        for (int i = 0; i < RING; i++) if (!issued[i]) {
            GL33.glBeginQuery(GL33.GL_TIME_ELAPSED, queries[i]); openSlot = i; queryOpen = true; return;
        }
        gpuDropped++;
    }
    public static void afterRender() {
        if (!activeFrame) return;
        activeFrame = false;
        if (queryOpen) { GL33.glEndQuery(GL33.GL_TIME_ELAPSED); issued[openSlot] = true; queryOpen = false; openSlot = -1; }
        samples.add(new Sample(System.nanoTime() - cpuStart, -1));
    }
    public static void swap() { if (enabled) swaps.add(System.nanoTime()); }
    public static void beginMeasurement() {
        enabled = true; activeFrame = false; samples.clear(); swaps.clear(); gpuValid = gpuDropped = 0;
        try {
            for (int i = 0; i < RING; i++) queries[i] = GL33.glGenQueries();
            gpuSupported = Arrays.stream(queries).allMatch(id -> id != 0);
        } catch (Throwable ignored) { gpuSupported = false; }
        if (!gpuSupported) cleanupQueries();
    }
    /** Captures the values the client is actually using; mismatch aborts the run before samples begin. */
    public static void verifyRuntimeContract(MinecraftClient client) {
        framebufferWidth = client.getWindow().getFramebufferWidth(); framebufferHeight = client.getWindow().getFramebufferHeight();
        fov = client.options.getFov().getValue(); renderDistance = client.options.getViewDistance().getValue();
        simulationDistance = client.options.getSimulationDistance().getValue(); vsync = client.options.getEnableVsync().getValue();
        fpsCap = ((MinecraftClientAccessor) client).ryoPerformance$getFramerateLimit();
        if (framebufferWidth != 1280 || framebufferHeight != 720 || fov != 70 || renderDistance != 12 || simulationDistance != 12 || vsync || fpsCap != 10000)
            throw new IllegalStateException("Benchmark runtime contract mismatch: framebuffer=" + framebufferWidth + "x" + framebufferHeight + ", fov=" + fov + ", render=" + renderDistance + ", simulation=" + simulationDistance + ", vsync=" + vsync + ", cap=" + fpsCap);
    }
    private static void collectAvailableQueries() {
        for (int i = 0; i < RING; i++) if (issued[i] && GL33.glGetQueryObjecti(queries[i], GL33.GL_QUERY_RESULT_AVAILABLE) != 0) {
            // Read only after availability: this is deliberately never a GPU synchronization point.
            long value = GL33.glGetQueryObjecti64(queries[i], GL33.GL_QUERY_RESULT);
            samples.add(new Sample(-1, value)); gpuValid++; issued[i] = false;
        }
    }
    public static void finishAndStop(MinecraftClient client) {
        if (!enabled) return;
        enabled = false;
        boolean screenshotQueued = false;
        try { collectAvailableQueries(); Path out = write(client.runDirectory.toPath());
            ScreenshotRecorder.saveScreenshot(out.toFile(), "native-framebuffer.png", client.getFramebuffer(), ignored -> client.scheduleStop());
            screenshotQueued = true;
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
        finally { cleanupQueries(); }
        if (!screenshotQueued) client.scheduleStop();
    }
    private static void cleanupQueries() { for (int i = 0; i < RING; i++) { if (queries[i] != 0) try { GL33.glDeleteQueries(queries[i]); } catch (Throwable ignored) {} queries[i]=0; issued[i]=false; } }
    static double percentile(long[] values, double p) { if (values.length == 0) return 0; Arrays.sort(values); return values[(int)Math.ceil(p * values.length) - 1]; }
    private static long[] valuesCpu() { return samples.stream().filter(s -> s.cpuNanos >= 0).mapToLong(Sample::cpuNanos).toArray(); }
    private static long[] valuesGpu() { return samples.stream().filter(s -> s.gpuNanos >= 0).mapToLong(Sample::gpuNanos).toArray(); }
    private static String stats(String name, long[] v) { return "\""+name+"\":{\"median\":"+percentile(v,.5)+",\"p95\":"+percentile(v,.95)+",\"p99\":"+percentile(v,.99)+",\"max\":"+percentile(v,1)+"}"; }
    private static Path write(Path run) throws IOException {
        Path out = Path.of(System.getProperty("ryoPerformance.output", run.resolve("benchmark-results").toString())); Files.createDirectories(out);
        try (var w = Files.newBufferedWriter(out.resolve("frames.csv"))) { w.write("record,cpu_nanos,gpu_nanos,swap_nanos\n"); int n=Math.max(samples.size(),swaps.size()); for(int i=0;i<n;i++){ Sample s=i<samples.size()?samples.get(i):new Sample(-1,-1); long sw=i<swaps.size()?swaps.get(i):-1; w.write(i+","+s.cpuNanos+","+s.gpuNanos+","+sw+"\n"); } }
        long[] intervals = new long[Math.max(0, swaps.size()-1)]; for(int i=1;i<swaps.size();i++) intervals[i-1]=swaps.get(i)-swaps.get(i-1);
        long[] cpu=valuesCpu(), gpu=valuesGpu(); long first=swaps.isEmpty()?0:swaps.get(0), last=swaps.isEmpty()?0:swaps.get(swaps.size()-1);
        long misses=Arrays.stream(intervals).filter(v -> v > 16_666_667L).count();
        String meta = System.getProperty("ryoPerformance.metadata", "{}").replace("\\", "\\\\").replace("\"", "\\\"");
        String json="{\"label\":\"hidden-window uncapped render-submission throughput; not presented FPS\",\"createdAt\":\""+Instant.now()+"\",\"variant\":\""+System.getProperty("ryoPerformance.variant","unknown")+"\",\"runId\":\""+System.getProperty("ryoPerformance.runId","unknown")+"\",\"warmupSeconds\":"+System.getProperty("ryoPerformance.warmupSeconds")+",\"measurementSeconds\":"+System.getProperty("ryoPerformance.measurementSeconds")+",\"completionState\":\"complete\",\"submittedFrames\":"+swaps.size()+",\"submittedFps\":"+(last>first?(swaps.size()-1)*1_000_000_000d/(last-first):0)+",\"swapCount\":"+swaps.size()+","+stats("frameIntervalNanos",intervals)+",\"frameBudgetMissesOver16_67ms\":"+misses+","+stats("renderCpuNanos",cpu)+",\"gpuTimerSupported\":"+gpuSupported+",\"gpuValidSampleCount\":"+gpu.length+",\"gpuDroppedSampleCount\":"+gpuDropped+","+stats("gpuNanos",gpu)+",\"runtimeContract\":{\"framebuffer\":\""+framebufferWidth+"x"+framebufferHeight+"\",\"fov\":"+fov+",\"renderDistance\":"+renderDistance+",\"simulationDistance\":"+simulationDistance+",\"vsync\":"+vsync+",\"fpsCap\":"+fpsCap+"},\"profileMetadataRaw\":\""+meta+"\"}";
        Files.writeString(out.resolve("report.json"), json);
        return out;
    }
}
