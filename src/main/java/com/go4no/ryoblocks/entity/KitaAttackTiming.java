package com.go4no.ryoblocks.entity;

public final class KitaAttackTiming {
    public static final int MAIN_INTERVAL = faster(40);
    public static final int STUCK_WAIT = faster(20);
    public static final int CLEARING_INTERVAL = faster(30);

    private KitaAttackTiming() { }

    public static int faster(int vanillaTicks) {
        return Math.max(1, Math.round(vanillaTicks / 1.5F));
    }
}
