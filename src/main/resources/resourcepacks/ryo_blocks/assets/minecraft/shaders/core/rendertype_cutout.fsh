#version 150

#moj_import <fog.glsl>

const float RYO_OVERLAY_STRENGTH = 0.50;
const float VANILLA_SPRITE_SIZE = 16.0;

uniform sampler2D Sampler0;
uniform sampler2D RyoSampler;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightColor;
in vec2 texCoord0;
in vec4 normal;
in vec3 worldPosition;
in vec3 worldNormal;

out vec4 fragColor;

void main() {
    vec4 atlasSample = texture(Sampler0, texCoord0);
    vec4 vanilla = atlasSample * vertexColor * ColorModulator;
    if (vanilla.a < 0.1) {
        discard;
    }
    vec2 atlasPixels = texCoord0 * vec2(textureSize(Sampler0, 0));
    vec4 ryo = texture(RyoSampler, fract(atlasPixels / VANILLA_SPRITE_SIZE));
    float overlayStrength = RYO_OVERLAY_STRENGTH * ryo.a;
    vec4 color = vec4(mix(vanilla.rgb, ryo.rgb, overlayStrength), vanilla.a) * lightColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
