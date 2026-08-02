package com.go4no.ryoblocks.client.terrain;

import net.minecraft.client.render.RenderLayer;

public enum TerrainLayerKind {
    SOLID,
    CUTOUT_MIPPED,
    CUTOUT;

    public RenderLayer renderLayer() {
        return switch (this) {
            case SOLID -> RenderLayer.getSolid();
            case CUTOUT_MIPPED -> RenderLayer.getCutoutMipped();
            case CUTOUT -> RenderLayer.getCutout();
        };
    }

    public static TerrainLayerKind from(RenderLayer layer) {
        if (layer == RenderLayer.getSolid()) {
            return SOLID;
        }
        if (layer == RenderLayer.getCutoutMipped()) {
            return CUTOUT_MIPPED;
        }
        if (layer == RenderLayer.getCutout()) {
            return CUTOUT;
        }
        return null;
    }
}
