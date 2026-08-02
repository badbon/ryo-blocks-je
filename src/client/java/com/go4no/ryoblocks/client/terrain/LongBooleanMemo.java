package com.go4no.ryoblocks.client.terrain;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.function.LongPredicate;

/** Small primitive memo scoped and reset by its caller. */
public final class LongBooleanMemo {
    private static final byte MISSING = -1;
    private static final byte FALSE = 0;
    private static final byte TRUE = 1;

    private final Long2ByteOpenHashMap values = new Long2ByteOpenHashMap();
    private Object owner;
    private long generation = Long.MIN_VALUE;

    public LongBooleanMemo() {
        values.defaultReturnValue(MISSING);
    }

    public boolean getOrCompute(long key, LongPredicate source) {
        byte cached = values.get(key);
        if (cached != MISSING) {
            return cached == TRUE;
        }
        boolean result = source.test(key);
        values.put(key, result ? TRUE : FALSE);
        return result;
    }

    public boolean getOrCompute(Object currentOwner, long currentGeneration, long key, LongPredicate source) {
        if (owner != currentOwner || generation != currentGeneration) {
            values.clear();
            owner = currentOwner;
            generation = currentGeneration;
        }
        return getOrCompute(key, source);
    }

    public void clear() {
        values.clear();
    }

    public void reset() {
        values.clear();
        owner = null;
        generation = Long.MIN_VALUE;
    }

    public int size() {
        return values.size();
    }
}
