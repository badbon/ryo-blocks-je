package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.RyoBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public final class RyoTerrainShaders {
    private static final String RYO_SAMPLER = "RyoSampler";
    private static final Identifier RYO_OVERLAY = new Identifier(RyoBlocks.MOD_ID, "textures/terrain/ryo_overlay.png");

    private RyoTerrainShaders() {
    }

    public static void bindTerrainSamplers() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        AbstractTexture overlay = client.getTextureManager().getTexture(RYO_OVERLAY);
        bind(GameRenderer.getRenderTypeSolidProgram(), overlay);
        bind(GameRenderer.getRenderTypeCutoutProgram(), overlay);
        bind(GameRenderer.getRenderTypeCutoutMippedProgram(), overlay);
        bind(GameRenderer.getRenderTypeTranslucentProgram(), overlay);
    }

    private static void bind(ShaderProgram program, AbstractTexture overlay) {
        if (program != null && overlay != null) {
            program.addSampler(RYO_SAMPLER, overlay);
        }
    }
}
