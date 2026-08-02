package com.go4no.ryoblocks.client.terrain;

import java.util.Map;
import java.util.TreeMap;

/** Deterministic first-fit allocator used by each fixed-size terrain VBO arena. */
public final class FreeListAllocator {
    private final int capacity;
    private final int alignment;
    private final TreeMap<Integer, Integer> freeBlocks = new TreeMap<>();

    public FreeListAllocator(int capacity, int alignment) {
        if (capacity <= 0 || alignment <= 0 || capacity % alignment != 0) {
            throw new IllegalArgumentException("capacity and alignment must be positive and aligned");
        }
        this.capacity = capacity;
        this.alignment = alignment;
        freeBlocks.put(0, capacity);
    }

    public Allocation allocate(int requestedBytes) {
        int size = align(requestedBytes);
        if (requestedBytes <= 0 || size > capacity) {
            return null;
        }
        for (Map.Entry<Integer, Integer> block : freeBlocks.entrySet()) {
            if (block.getValue() >= size) {
                int offset = block.getKey();
                int remainder = block.getValue() - size;
                freeBlocks.remove(offset);
                if (remainder != 0) {
                    freeBlocks.put(offset + size, remainder);
                }
                return new Allocation(offset, size);
            }
        }
        return null;
    }

    public void free(Allocation allocation) {
        if (allocation == null || allocation.offset < 0 || allocation.size <= 0
            || allocation.offset % alignment != 0 || allocation.size % alignment != 0
            || allocation.offset + allocation.size > capacity) {
            throw new IllegalArgumentException("invalid allocation");
        }

        int start = allocation.offset;
        int size = allocation.size;
        Map.Entry<Integer, Integer> lower = freeBlocks.floorEntry(start);
        if (lower != null) {
            int lowerEnd = lower.getKey() + lower.getValue();
            if (lowerEnd > start) {
                throw new IllegalStateException("allocation overlaps an existing free block");
            }
            if (lowerEnd == start) {
                start = lower.getKey();
                size += lower.getValue();
                freeBlocks.remove(lower.getKey());
            }
        }

        Map.Entry<Integer, Integer> higher = freeBlocks.ceilingEntry(start);
        if (higher != null) {
            int end = start + size;
            if (end > higher.getKey()) {
                throw new IllegalStateException("allocation overlaps an existing free block");
            }
            if (end == higher.getKey()) {
                size += higher.getValue();
                freeBlocks.remove(higher.getKey());
            }
        }
        freeBlocks.put(start, size);
    }

    public int freeBytes() {
        return freeBlocks.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int largestFreeBlock() {
        return freeBlocks.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private int align(int bytes) {
        long aligned = ((long) bytes + alignment - 1L) / alignment * alignment;
        return aligned > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) aligned;
    }

    public record Allocation(int offset, int size) {
    }
}
