package com.go4no.ryoblocks.client.terrain;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;

/**
 * Mirrors vanilla chunk uploads into persistent shared VBO arenas. Vanilla's
 * buffers remain authoritative and are used whenever a complete safe batch is
 * unavailable.
 */
public final class TerrainBatcher {
    private static final TerrainBatcher INSTANCE = new TerrainBatcher();
    private static final int ARENA_BYTES = 16 * 1024 * 1024;

    private final IdentityHashMap<VertexBuffer, Binding> bindings = new IdentityHashMap<>();
    private final IdentityHashMap<ChunkBuilder.BuiltChunk, EnumMap<TerrainLayerKind, Mesh>> meshes = new IdentityHashMap<>();
    private final EnumMap<TerrainLayerKind, List<Arena>> arenas = new EnumMap<>(TerrainLayerKind.class);
    private final EnumMap<TerrainLayerKind, LayerCommands> commands = new EnumMap<>(TerrainLayerKind.class);
    private final ArrayList<ChunkBuilder.BuiltChunk> visibleChunks = new ArrayList<>();

    private ByteBuffer scratch = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
    private long uploadGeneration;
    private long cachedUploadGeneration = -1L;
    private boolean supported;
    private boolean supportChecked;

    private TerrainBatcher() {
        for (TerrainLayerKind kind : TerrainLayerKind.values()) {
            arenas.put(kind, new ArrayList<>());
            commands.put(kind, LayerCommands.invalid());
        }
    }

    public static TerrainBatcher get() {
        return INSTANCE;
    }

    public void trackBuffer(ChunkBuilder.BuiltChunk chunk, RenderLayer layer, VertexBuffer buffer) {
        TerrainLayerKind kind = TerrainLayerKind.from(layer);
        if (kind != null && buffer != null) {
            // Chunk rebuild workers resolve the destination buffer before the
            // queued upload is executed on the render thread.
            synchronized (bindings) {
                bindings.put(buffer, new Binding(chunk, kind));
            }
        }
    }

    public void untrackBuffer(VertexBuffer buffer) {
        synchronized (bindings) {
            bindings.remove(buffer);
        }
    }

    public void mirrorUpload(VertexBuffer target, BufferBuilder.BuiltBuffer builtBuffer) {
        RenderSystem.assertOnRenderThread();
        Binding binding;
        synchronized (bindings) {
            binding = bindings.get(target);
        }
        if (binding == null || !ensureSupported()) {
            return;
        }

        BufferBuilder.DrawParameters parameters = builtBuffer.getParameters();
        if (!isCompatible(parameters)) {
            removeMesh(binding.chunk, binding.kind);
            return;
        }

        int bytes = parameters.getVertexBufferSize();
        ArenaAllocation placement = null;
        try {
            placement = allocate(binding.kind, bytes);
            if (placement == null) {
                // A replacement upload may be the only allocation keeping an
                // arena full. Release that stale copy and retry once.
                removeMesh(binding.chunk, binding.kind);
                placement = allocate(binding.kind, bytes);
            }
            if (placement == null) {
                return;
            }

            BlockPos origin = binding.chunk.getOrigin();
            int anchorX = TerrainBatchMath.pageAnchor(origin.getX());
            int anchorY = TerrainBatchMath.pageAnchor(origin.getY());
            int anchorZ = TerrainBatchMath.pageAnchor(origin.getZ());
            ByteBuffer upload = translatedVertices(
                builtBuffer.getVertexBuffer(), parameters.vertexCount(), origin, anchorX, anchorY, anchorZ
            );
            placement.arena.upload(placement.allocation.offset(), upload);

            EnumMap<TerrainLayerKind, Mesh> byLayer = meshes.computeIfAbsent(
                binding.chunk, ignored -> new EnumMap<>(TerrainLayerKind.class)
            );
            Mesh previous = byLayer.put(binding.kind, new Mesh(
                placement.arena,
                placement.allocation,
                parameters.vertexCount(),
                parameters.indexCount(),
                anchorX,
                anchorY,
                anchorZ,
                origin.asLong(),
                false
            ));
            if (previous != null) {
                previous.arena.allocator.free(previous.allocation);
            }
            uploadGeneration++;
        } catch (RuntimeException | LinkageError | OutOfMemoryError failure) {
            if (placement != null) {
                placement.arena.allocator.free(placement.allocation);
            }
            disableAfterMirrorFailure();
        }
    }

    public void commitChunk(ChunkBuilder.BuiltChunk chunk) {
        EnumMap<TerrainLayerKind, Mesh> byLayer = meshes.get(chunk);
        for (TerrainLayerKind kind : TerrainLayerKind.values()) {
            if (chunk.getData().isEmpty(kind.renderLayer())) {
                removeMesh(chunk, kind);
            } else if (byLayer != null) {
                Mesh mesh = byLayer.get(kind);
                if (mesh != null && mesh.origin == chunk.getOrigin().asLong() && !mesh.committed) {
                    byLayer.put(kind, mesh.asCommitted());
                    uploadGeneration++;
                }
            }
        }
    }

    public void removeChunk(ChunkBuilder.BuiltChunk chunk) {
        EnumMap<TerrainLayerKind, Mesh> removed = meshes.remove(chunk);
        if (removed != null) {
            for (Mesh mesh : removed.values()) {
                mesh.arena.allocator.free(mesh.allocation);
            }
            uploadGeneration++;
        }
        synchronized (bindings) {
            bindings.entrySet().removeIf(entry -> entry.getValue().chunk == chunk);
        }
    }

    public void beginFrame(ObjectArrayList<?> chunkInfos) {
        if (!ensureSupported()) {
            return;
        }
        boolean visibilityChanged = chunkInfos.size() != visibleChunks.size();
        if (!visibilityChanged) {
            for (int i = 0; i < chunkInfos.size(); i++) {
                if (chunkFromInfo(chunkInfos.get(i)) != visibleChunks.get(i)) {
                    visibilityChanged = true;
                    break;
                }
            }
        }
        if (visibilityChanged) {
            visibleChunks.clear();
            for (Object chunkInfo : chunkInfos) {
                visibleChunks.add(chunkFromInfo(chunkInfo));
            }
        }
        if (TerrainBatchContracts.requiresCommandRebuild(visibilityChanged, cachedUploadGeneration, uploadGeneration)) {
            for (TerrainLayerKind kind : TerrainLayerKind.values()) {
                commands.put(kind, buildCommands(kind));
            }
            cachedUploadGeneration = uploadGeneration;
        }
    }

    public boolean render(
        RenderLayer layer,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ,
        Matrix4f projectionMatrix
    ) {
        TerrainLayerKind kind = TerrainLayerKind.from(layer);
        if (kind == null || !supported) {
            return false;
        }
        LayerCommands layerCommands = commands.get(kind);
        if (!TerrainBatchContracts.canReplaceVanilla(layerCommands.valid, layerCommands.runs.size())) {
            return false;
        }

        RenderSystem.assertOnRenderThread();
        layer.startDrawing();
        ShaderProgram shader = RenderSystem.getShader();
        if (shader == null || shader.chunkOffset == null) {
            layer.endDrawing();
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.getProfiler().push("filterempty");
        client.getProfiler().swap(() -> "render_ryo_batch_" + layer);
        setupShader(shader, matrices, projectionMatrix);
        shader.bind();
        GlUniform chunkOffset = shader.chunkOffset;

        for (DrawRun run : layerCommands.runs) {
            chunkOffset.set(
                TerrainBatchMath.cameraOffset(run.anchorX, cameraX),
                TerrainBatchMath.cameraOffset(run.anchorY, cameraY),
                TerrainBatchMath.cameraOffset(run.anchorZ, cameraZ)
            );
            chunkOffset.upload();
            run.arena.bind();
            RenderSystem.ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
            indices.bindAndGrow(run.maximumIndexCount);
            run.rewind();
            GL32C.glMultiDrawElementsBaseVertex(
                GL15C.GL_TRIANGLES,
                run.counts,
                indices.getIndexType().glType,
                run.indexPointers,
                run.baseVertices
            );
        }

        chunkOffset.set(0.0F, 0.0F, 0.0F);
        shader.unbind();
        VertexBuffer.unbind();
        client.getProfiler().pop();
        layer.endDrawing();
        return true;
    }

    public void reset() {
        synchronized (bindings) {
            bindings.clear();
        }
        meshes.clear();
        visibleChunks.clear();
        for (List<Arena> layerArenas : arenas.values()) {
            for (Arena arena : layerArenas) {
                arena.close();
            }
            layerArenas.clear();
        }
        for (TerrainLayerKind kind : TerrainLayerKind.values()) {
            commands.put(kind, LayerCommands.invalid());
        }
        uploadGeneration++;
        cachedUploadGeneration = -1L;
        supportChecked = false;
        supported = false;
    }

    private boolean ensureSupported() {
        if (!supportChecked) {
            supportChecked = true;
            try {
                supported = GL.getCapabilities().OpenGL32;
            } catch (IllegalStateException exception) {
                supported = false;
            }
        }
        return supported;
    }

    private static boolean isCompatible(BufferBuilder.DrawParameters parameters) {
        return !parameters.indexOnly()
            && parameters.sequentialIndex()
            && parameters.mode() == VertexFormat.DrawMode.QUADS
            && parameters.format().equals(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL)
            && parameters.format().getVertexSizeByte() == TerrainBatchMath.VERTEX_STRIDE
            && parameters.vertexCount() % 4 == 0
            && parameters.indexCount() == parameters.vertexCount() / 4 * 6;
    }

    private ByteBuffer translatedVertices(
        ByteBuffer source,
        int vertexCount,
        BlockPos origin,
        int anchorX,
        int anchorY,
        int anchorZ
    ) {
        int bytes = vertexCount * TerrainBatchMath.VERTEX_STRIDE;
        if (scratch.capacity() < bytes) {
            scratch = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        }
        scratch.clear().limit(bytes);
        ByteBuffer sourceView = source.duplicate();
        sourceView.clear().limit(bytes);
        scratch.put(sourceView);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int offset = vertex * TerrainBatchMath.VERTEX_STRIDE;
            scratch.putFloat(offset, TerrainBatchMath.bakedPosition(scratch.getFloat(offset), origin.getX(), anchorX));
            scratch.putFloat(offset + 4, TerrainBatchMath.bakedPosition(scratch.getFloat(offset + 4), origin.getY(), anchorY));
            scratch.putFloat(offset + 8, TerrainBatchMath.bakedPosition(scratch.getFloat(offset + 8), origin.getZ(), anchorZ));
        }
        scratch.position(0).limit(bytes);
        return scratch;
    }

    private ArenaAllocation allocate(TerrainLayerKind kind, int bytes) {
        if (bytes <= 0 || bytes > ARENA_BYTES) {
            return null;
        }
        for (Arena arena : arenas.get(kind)) {
            FreeListAllocator.Allocation allocation = arena.allocator.allocate(bytes);
            if (allocation != null) {
                return new ArenaAllocation(arena, allocation);
            }
        }
        Arena arena = new Arena();
        arenas.get(kind).add(arena);
        return new ArenaAllocation(arena, arena.allocator.allocate(bytes));
    }

    private void removeMesh(ChunkBuilder.BuiltChunk chunk, TerrainLayerKind kind) {
        EnumMap<TerrainLayerKind, Mesh> byLayer = meshes.get(chunk);
        if (byLayer == null) {
            return;
        }
        Mesh removed = byLayer.remove(kind);
        if (removed != null) {
            removed.arena.allocator.free(removed.allocation);
            uploadGeneration++;
        }
        if (byLayer.isEmpty()) {
            meshes.remove(chunk);
        }
    }

    private void disableAfterMirrorFailure() {
        // Do not perform more GL work from a failing upload hook. The original
        // VertexBuffer.upload continues immediately after this method returns.
        supported = false;
        supportChecked = true;
        meshes.clear();
        visibleChunks.clear();
        for (TerrainLayerKind kind : TerrainLayerKind.values()) {
            commands.put(kind, LayerCommands.invalid());
        }
        uploadGeneration++;
        cachedUploadGeneration = -1L;
    }

    private LayerCommands buildCommands(TerrainLayerKind kind) {
        ArrayList<RunBuilder> builders = new ArrayList<>();
        RunBuilder current = null;
        for (ChunkBuilder.BuiltChunk chunk : visibleChunks) {
            if (chunk.getData().isEmpty(kind.renderLayer())) {
                continue;
            }
            EnumMap<TerrainLayerKind, Mesh> byLayer = meshes.get(chunk);
            Mesh mesh = byLayer == null ? null : byLayer.get(kind);
            if (mesh == null || !mesh.committed || mesh.origin != chunk.getOrigin().asLong()) {
                return LayerCommands.invalid();
            }
            if (current == null || !current.matches(mesh)) {
                current = new RunBuilder(mesh);
                builders.add(current);
            }
            current.add(mesh);
        }
        ArrayList<DrawRun> runs = new ArrayList<>(builders.size());
        for (RunBuilder builder : builders) {
            runs.add(builder.build());
        }
        return new LayerCommands(true, List.copyOf(runs));
    }

    private static ChunkBuilder.BuiltChunk chunkFromInfo(Object chunkInfo) {
        return ((WorldRendererChunkInfoAccess) chunkInfo).ryoBlocks$getChunk();
    }

    private static void setupShader(ShaderProgram shader, MatrixStack matrices, Matrix4f projectionMatrix) {
        for (int sampler = 0; sampler < 12; sampler++) {
            shader.addSampler("Sampler" + sampler, RenderSystem.getShaderTexture(sampler));
        }
        if (shader.modelViewMat != null) shader.modelViewMat.set(matrices.peek().getPositionMatrix());
        if (shader.projectionMat != null) shader.projectionMat.set(projectionMatrix);
        if (shader.colorModulator != null) shader.colorModulator.set(RenderSystem.getShaderColor());
        if (shader.glintAlpha != null) shader.glintAlpha.set(RenderSystem.getShaderGlintAlpha());
        if (shader.fogStart != null) shader.fogStart.set(RenderSystem.getShaderFogStart());
        if (shader.fogEnd != null) shader.fogEnd.set(RenderSystem.getShaderFogEnd());
        if (shader.fogColor != null) shader.fogColor.set(RenderSystem.getShaderFogColor());
        if (shader.fogShape != null) shader.fogShape.set(RenderSystem.getShaderFogShape().getId());
        if (shader.textureMat != null) shader.textureMat.set(RenderSystem.getTextureMatrix());
        if (shader.gameTime != null) shader.gameTime.set(RenderSystem.getShaderGameTime());
        RenderSystem.setupShaderLights(shader);
    }

    private record Binding(ChunkBuilder.BuiltChunk chunk, TerrainLayerKind kind) {
    }

    private record ArenaAllocation(Arena arena, FreeListAllocator.Allocation allocation) {
    }

    private record Mesh(
        Arena arena,
        FreeListAllocator.Allocation allocation,
        int vertexCount,
        int indexCount,
        int anchorX,
        int anchorY,
        int anchorZ,
        long origin,
        boolean committed
    ) {
        Mesh asCommitted() {
            return new Mesh(arena, allocation, vertexCount, indexCount, anchorX, anchorY, anchorZ, origin, true);
        }
    }

    private record LayerCommands(boolean valid, List<DrawRun> runs) {
        static LayerCommands invalid() {
            return new LayerCommands(false, List.of());
        }
    }

    private static final class RunBuilder {
        private final Arena arena;
        private final int anchorX;
        private final int anchorY;
        private final int anchorZ;
        private final ArrayList<Mesh> meshes = new ArrayList<>();

        RunBuilder(Mesh first) {
            arena = first.arena;
            anchorX = first.anchorX;
            anchorY = first.anchorY;
            anchorZ = first.anchorZ;
        }

        boolean matches(Mesh mesh) {
            return TerrainBatchContracts.sameRun(
                arena, anchorX, anchorY, anchorZ,
                mesh.arena, mesh.anchorX, mesh.anchorY, mesh.anchorZ
            );
        }

        void add(Mesh mesh) {
            meshes.add(mesh);
        }

        DrawRun build() {
            IntBuffer counts = BufferUtils.createIntBuffer(meshes.size());
            IntBuffer baseVertices = BufferUtils.createIntBuffer(meshes.size());
            PointerBuffer pointers = BufferUtils.createPointerBuffer(meshes.size());
            int maximum = 0;
            for (Mesh mesh : meshes) {
                counts.put(mesh.indexCount);
                baseVertices.put(TerrainBatchMath.baseVertex(mesh.allocation.offset()));
                pointers.put(0L);
                maximum = Math.max(maximum, mesh.indexCount);
            }
            counts.flip();
            baseVertices.flip();
            pointers.flip();
            return new DrawRun(arena, anchorX, anchorY, anchorZ, counts, baseVertices, pointers, maximum);
        }
    }

    private record DrawRun(
        Arena arena,
        int anchorX,
        int anchorY,
        int anchorZ,
        IntBuffer counts,
        IntBuffer baseVertices,
        PointerBuffer indexPointers,
        int maximumIndexCount
    ) {
        void rewind() {
            counts.position(0);
            baseVertices.position(0);
            indexPointers.position(0);
        }
    }

    private static final class Arena {
        private final FreeListAllocator allocator = new FreeListAllocator(ARENA_BYTES, TerrainBatchMath.VERTEX_STRIDE);
        private final int vao;
        private final int vbo;

        Arena() {
            int createdVao = 0;
            int createdVbo = 0;
            GlBindingState previous = GlBindingState.capture();
            try {
                createdVao = GL30C.glGenVertexArrays();
                createdVbo = GL15C.glGenBuffers();
                GL30C.glBindVertexArray(createdVao);
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, createdVbo);
                GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, (long) ARENA_BYTES, GL15C.GL_STATIC_DRAW);
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.setupState();
            } catch (RuntimeException | LinkageError | OutOfMemoryError failure) {
                try {
                    if (createdVbo != 0) GL15C.glDeleteBuffers(createdVbo);
                    if (createdVao != 0) GL30C.glDeleteVertexArrays(createdVao);
                } catch (RuntimeException | LinkageError | OutOfMemoryError ignored) {
                    // Do not mask the allocation failure; the mirror is disabled.
                }
                throw failure;
            } finally {
                previous.restore();
            }
            vao = createdVao;
            vbo = createdVbo;
        }

        void upload(int byteOffset, ByteBuffer data) {
            GlBindingState previous = GlBindingState.capture();
            try {
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
                GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, byteOffset, data);
            } finally {
                previous.restore();
            }
        }

        void bind() {
            GL30C.glBindVertexArray(vao);
        }

        void close() {
            GlBindingState previous = null;
            try {
                GL.getCapabilities();
                previous = GlBindingState.capture();
                GL15C.glDeleteBuffers(vbo);
                GL30C.glDeleteVertexArrays(vao);
            } catch (RuntimeException | LinkageError | OutOfMemoryError ignored) {
                // The GL context is already gone; the driver owns its objects.
            } finally {
                if (previous != null) {
                    previous.restoreDeleted(vao, vbo);
                }
            }
        }
    }

    private record GlBindingState(int vertexArray, int arrayBuffer) {
        static GlBindingState capture() {
            return new GlBindingState(
                GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING),
                GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
            );
        }

        void restore() {
            restore(vertexArray, arrayBuffer);
        }

        void restoreDeleted(int deletedVertexArray, int deletedArrayBuffer) {
            restore(vertexArray == deletedVertexArray ? 0 : vertexArray, arrayBuffer == deletedArrayBuffer ? 0 : arrayBuffer);
        }

        private static void restore(int vertexArray, int arrayBuffer) {
            try {
                GL30C.glBindVertexArray(vertexArray);
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, arrayBuffer);
            } catch (RuntimeException | LinkageError | OutOfMemoryError ignored) {
                // Restoration is best effort during a lost-context/driver failure.
            }
        }
    }
}
