package com.go4no.ryoblocks.client.terrain;

/** Pure coordinate contracts for precision-safe page-relative terrain vertices. */
public final class TerrainBatchMath {
    public static final int PAGE_SIZE = 256;
    public static final int VERTEX_STRIDE = 32;

    private TerrainBatchMath() {
    }

    public static int pageAnchor(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, PAGE_SIZE) * PAGE_SIZE;
    }

    public static float bakedPosition(float localPosition, int sectionOrigin, int pageAnchor) {
        return localPosition + (sectionOrigin - pageAnchor);
    }

    public static float cameraOffset(int pageAnchor, double cameraCoordinate) {
        return (float) (pageAnchor - cameraCoordinate);
    }

    public static int baseVertex(int byteOffset) {
        if (byteOffset < 0 || byteOffset % VERTEX_STRIDE != 0) {
            throw new IllegalArgumentException("terrain allocation must be vertex aligned");
        }
        return byteOffset / VERTEX_STRIDE;
    }
}
