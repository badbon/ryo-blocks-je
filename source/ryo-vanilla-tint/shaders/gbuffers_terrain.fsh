#version 330 compatibility

const float RYO_OVERLAY_STRENGTH = 0.88;
const float VANILLA_SPRITE_SIZE = 16.0;

uniform sampler2D gtexture;
uniform sampler2D lightmap;
uniform sampler2D ryoTexture;
uniform float alphaTestRef;

in vec2 atlasCoord;
in vec2 lightmapCoord;
in vec4 vertexColor;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 color;

void main() {
    vec4 vanilla = texture(gtexture, atlasCoord) * vertexColor;
    if (vanilla.a < alphaTestRef) {
        discard;
    }

    // Vanilla block sprites are packed on a 16px atlas grid. Recovering the
    // local fragment coordinate here maps the complete Ryo image to each
    // vanilla sprite without allocating a high-resolution copy per block.
    vec2 atlasPixels = atlasCoord * vec2(textureSize(gtexture, 0));
    vec2 ryoCoord = fract(atlasPixels / VANILLA_SPRITE_SIZE);
    vec3 ryo = texture(ryoTexture, ryoCoord).rgb;

    vec4 tinted = vec4(mix(vanilla.rgb, ryo, RYO_OVERLAY_STRENGTH), vanilla.a);
    color = tinted * texture(lightmap, lightmapCoord);
}
