package com.go4no.ryoblocks.client;

import com.go4no.ryoblocks.RyoBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public final class RyoTerrainShaders {
    private static final String RYO_SAMPLER = "RyoSampler";
    private static final String KITA_SAMPLER = "KitaSampler";
    private static final Identifier RYO_OVERLAY = new Identifier(RyoBlocks.MOD_ID, "textures/terrain/ryo_overlay.png");
    private static final Identifier KITA_LAVA = new Identifier(RyoBlocks.MOD_ID, "textures/terrain/kita_lava.png");

    private RyoTerrainShaders() {
    }

    public static void bindTerrainSamplers() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        AbstractTexture overlay = client.getTextureManager().getTexture(RYO_OVERLAY);
        AbstractTexture kitaLava = client.getTextureManager().getTexture(KITA_LAVA);
        bind(GameRenderer.getRenderTypeSolidProgram(), overlay, kitaLava);
        bind(GameRenderer.getRenderTypeCutoutProgram(), overlay, kitaLava);
        bind(GameRenderer.getRenderTypeCutoutMippedProgram(), overlay, kitaLava);
        bind(GameRenderer.getRenderTypeTranslucentProgram(), overlay, kitaLava);
    }

    private static void bind(ShaderProgram program, AbstractTexture overlay, AbstractTexture kitaLava) {
        if (program != null && overlay != null && kitaLava != null) {
            program.addSampler(RYO_SAMPLER, overlay);
            program.addSampler(KITA_SAMPLER, kitaLava);
        }
    }
}
