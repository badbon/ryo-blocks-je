package com.go4no.ryoblocks.entity;

/** Dependency-free timing contracts, executed by verifyKitaAttackTiming. */
public final class KitaAttackTimingTest {
    public static void main(String[] args) {
        check(KitaAttackTiming.MAIN_INTERVAL == 27, "main attack");
        check(KitaAttackTiming.STUCK_WAIT == 13, "initial stuck wait");
        check(KitaAttackTiming.CLEARING_INTERVAL == 20, "clearing burst");
        check(KitaAttackTiming.faster(40) == 27 && KitaAttackTiming.faster(59) == 39, "secondary range");
        check(KitaAttackTiming.faster(10) == 7 && KitaAttackTiming.faster(19) == 13, "idle check range");
        for (int ticks = 1; ticks <= 323; ticks++) {
            int result = KitaAttackTiming.faster(ticks);
            check(result >= 1 && Math.abs(result - ticks / 1.5) <= 0.5, "nearest tick for " + ticks);
        }
        System.out.println("Kita firing timing contracts passed");
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
