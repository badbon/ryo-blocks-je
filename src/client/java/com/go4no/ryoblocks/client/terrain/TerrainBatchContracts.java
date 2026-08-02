package com.go4no.ryoblocks.client.terrain;

import java.util.ArrayList;
import java.util.List;

/** Pure, deterministic contracts shared by the runtime batch cache and tests. */
public final class TerrainBatchContracts {
    private TerrainBatchContracts() {
    }

    public static boolean requiresCommandRebuild(
        boolean visibilityChanged,
        long cachedUploadGeneration,
        long uploadGeneration
    ) {
        return visibilityChanged || cachedUploadGeneration != uploadGeneration;
    }

    public static boolean canReplaceVanilla(boolean complete, int drawRunCount) {
        return complete && drawRunCount > 0;
    }

    public static boolean sameRun(
        Object leftArena,
        int leftX,
        int leftY,
        int leftZ,
        Object rightArena,
        int rightX,
        int rightY,
        int rightZ
    ) {
        return leftArena == rightArena && leftX == rightX && leftY == rightY && leftZ == rightZ;
    }

    /** Returns contiguous ranges; concatenating them always preserves input order. */
    public static List<Range> contiguousRanges(int[] keys) {
        if (keys.length == 0) {
            return List.of();
        }
        ArrayList<Range> ranges = new ArrayList<>();
        int start = 0;
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] != keys[i - 1]) {
                ranges.add(new Range(start, i));
                start = i;
            }
        }
        ranges.add(new Range(start, keys.length));
        return List.copyOf(ranges);
    }

    public record Range(int startInclusive, int endExclusive) {
        public Range {
            if (startInclusive < 0 || endExclusive <= startInclusive) {
                throw new IllegalArgumentException("invalid contiguous range");
            }
        }
    }
}
