package com.go4no.ryoblocks.client.terrain;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free deterministic contract suite; run with assertions enabled. */
public final class TerrainBatchContractsTest {
    public static void main(String[] args) {
        allocatorCoalescesAndReusesFirstFit();
        generationsInvalidateOnlyWhenRequired();
        contiguousRunsPreserveCommandOrder();
        pageOffsetsAndBaseVerticesAreExact();
        lifecycleFailsClosed();
        lightingColumnsReuseUntilGenerationOrProviderChanges();
        System.out.println("Terrain batch contracts passed");
    }

    private static void allocatorCoalescesAndReusesFirstFit() {
        FreeListAllocator allocator = new FreeListAllocator(320, 32);
        FreeListAllocator.Allocation first = require(allocator.allocate(33));
        FreeListAllocator.Allocation second = require(allocator.allocate(64));
        FreeListAllocator.Allocation third = require(allocator.allocate(32));
        check(first.offset() == 0 && first.size() == 64, "alignment");
        check(second.offset() == 64 && third.offset() == 128, "deterministic first fit");
        allocator.free(second);
        allocator.free(first);
        FreeListAllocator.Allocation merged = require(allocator.allocate(96));
        check(merged.offset() == 0, "left coalescing and first-fit reuse");
        allocator.free(merged);
        allocator.free(third);
        check(allocator.freeBytes() == 320 && allocator.largestFreeBlock() == 320, "full coalescing");
    }

    private static void generationsInvalidateOnlyWhenRequired() {
        check(!TerrainBatchContracts.requiresCommandRebuild(false, 7, 7), "stable cache reuse");
        check(TerrainBatchContracts.requiresCommandRebuild(true, 7, 7), "visibility invalidation");
        check(TerrainBatchContracts.requiresCommandRebuild(false, 7, 8), "upload invalidation");
    }

    private static void contiguousRunsPreserveCommandOrder() {
        int[] orderedKeys = {1, 1, 2, 1, 3, 3};
        List<TerrainBatchContracts.Range> ranges = TerrainBatchContracts.contiguousRanges(orderedKeys);
        check(ranges.equals(List.of(
            new TerrainBatchContracts.Range(0, 2),
            new TerrainBatchContracts.Range(2, 3),
            new TerrainBatchContracts.Range(3, 4),
            new TerrainBatchContracts.Range(4, 6)
        )), "contiguous grouping must not reorder returning page keys");
        int cursor = 0;
        for (TerrainBatchContracts.Range range : ranges) {
            check(range.startInclusive() == cursor, "no ordering gap");
            cursor = range.endExclusive();
        }
        check(cursor == orderedKeys.length, "all commands retained");
    }

    private static void pageOffsetsAndBaseVerticesAreExact() {
        check(TerrainBatchMath.pageAnchor(-1) == -256, "negative page floor");
        check(TerrainBatchMath.pageAnchor(256) == 256, "positive page boundary");
        check(TerrainBatchMath.baseVertex(96) == 3, "base vertex math");
        float local = 15.5F;
        int origin = -17;
        int anchor = TerrainBatchMath.pageAnchor(origin);
        double camera = -31.25;
        float reconstructed = TerrainBatchMath.bakedPosition(local, origin, anchor)
            + TerrainBatchMath.cameraOffset(anchor, camera);
        check(reconstructed == (float) (local + origin - camera), "page-relative reconstruction");
    }

    private static void lifecycleFailsClosed() {
        check(!TerrainBatchContracts.canReplaceVanilla(false, 4), "pending or failed upload fallback");
        check(!TerrainBatchContracts.canReplaceVanilla(true, 0), "empty command fallback");
        check(TerrainBatchContracts.canReplaceVanilla(true, 1), "committed complete batch");
        check(TerrainLayerKind.values().length == 3, "translucent and tripwire must never enter the batch policy");
    }

    private static void lightingColumnsReuseUntilGenerationOrProviderChanges() {
        LongBooleanMemo memo = new LongBooleanMemo();
        AtomicInteger providerCalls = new AtomicInteger();
        Object firstProvider = new Object();
        Object secondProvider = new Object();

        boolean firstSection = memo.getOrCompute(firstProvider, 7L, 101L, ignored -> {
            providerCalls.incrementAndGet();
            return true;
        });
        boolean sameColumnNextUpdate = memo.getOrCompute(firstProvider, 7L, 101L, ignored -> {
            providerCalls.incrementAndGet();
            return false;
        });
        boolean differentColumn = memo.getOrCompute(firstProvider, 7L, 202L, ignored -> {
            providerCalls.incrementAndGet();
            return false;
        });
        check(firstSection && sameColumnNextUpdate, "same-column cross-call reuse at one generation");
        check(!differentColumn, "different columns preserve independent provider results");
        check(providerCalls.get() == 2 && memo.size() == 2, "one provider query per column");

        boolean nextGeneration = memo.getOrCompute(firstProvider, 8L, 101L, ignored -> {
            providerCalls.incrementAndGet();
            return false;
        });
        check(!nextGeneration, "generation invalidation preserves a fresh false result");
        check(providerCalls.get() == 3 && memo.size() == 1, "generation change clears old columns");

        boolean nextProvider = memo.getOrCompute(secondProvider, 8L, 101L, ignored -> {
            providerCalls.incrementAndGet();
            return true;
        });
        check(nextProvider, "provider identity invalidation preserves a fresh true result");
        check(providerCalls.get() == 4 && memo.size() == 1, "provider change clears old columns");

        memo.reset();
        boolean afterReset = memo.getOrCompute(secondProvider, 8L, 101L, ignored -> {
            providerCalls.incrementAndGet();
            return false;
        });
        check(!afterReset && providerCalls.get() == 5, "explicit lifecycle reset prevents reuse");
    }

    private static FreeListAllocator.Allocation require(FreeListAllocator.Allocation allocation) {
        if (allocation == null) {
            throw new AssertionError("allocation unexpectedly failed");
        }
        return allocation;
    }

    private static void check(boolean condition, String contract) {
        if (!condition) {
            throw new AssertionError(contract);
        }
    }
}
