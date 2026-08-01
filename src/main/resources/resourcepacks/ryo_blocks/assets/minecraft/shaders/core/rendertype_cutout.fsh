#version 150

#moj_import <fog.glsl>

const float RYO_OVERLAY_STRENGTH = 0.50;
const float VANILLA_SPRITE_SIZE = 16.0;
const float KITA_LAVA_OVERLAY_STRENGTH = 0.94;
const float KITA_STILL_MARKER_ALPHA = 254.0 / 255.0;
const float KITA_FLOW_MARKER_ALPHA = 253.0 / 255.0;
const float KITA_MARKER_TOLERANCE = 0.0005;
const float KITA_FACE_WORLD_SCALE = 1.0;
const vec2 KITA_CROP_ORIGIN = vec2(0.2177, 0.0);
const vec2 KITA_CROP_SIZE = vec2(0.5628, 1.0);

uniform sampler2D Sampler0;
uniform sampler2D RyoSampler;
uniform sampler2D KitaSampler;

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

vec2 kitaFaceUv(vec3 position) {
    vec3 absoluteDirection = abs(normalize(cross(dFdx(position), dFdy(position))));
    if (absoluteDirection.y >= max(absoluteDirection.x, absoluteDirection.z)) {
        return fract(vec2(position.x, position.z) / KITA_FACE_WORLD_SCALE);
    }
    if (absoluteDirection.x >= absoluteDirection.z) {
        return fract(vec2(position.z, -position.y) / KITA_FACE_WORLD_SCALE);
    }
    return fract(vec2(position.x, -position.y) / KITA_FACE_WORLD_SCALE);
}

void main() {
    vec4 atlasSample = texture(Sampler0, texCoord0);
    vec4 vanilla = atlasSample * vertexColor * ColorModulator;
    if (vanilla.a < 0.1) {
        discard;
    }
    vec2 atlasPixels = texCoord0 * vec2(textureSize(Sampler0, 0));
    bool kitaStill = abs(atlasSample.a - KITA_STILL_MARKER_ALPHA) < KITA_MARKER_TOLERANCE;
    bool kitaFlow = abs(atlasSample.a - KITA_FLOW_MARKER_ALPHA) < KITA_MARKER_TOLERANCE;
    vec4 color;
    if (kitaStill || kitaFlow) {
        vec2 spriteUv = kitaFaceUv(worldPosition);
        vec2 kitaUv = KITA_CROP_ORIGIN + spriteUv * KITA_CROP_SIZE;
        vec4 kita = texture(KitaSampler, kitaUv);
        float overlayStrength = KITA_LAVA_OVERLAY_STRENGTH * kita.a;
        color = vec4(mix(vanilla.rgb, kita.rgb, overlayStrength), vertexColor.a * ColorModulator.a) * lightColor;
    } else {
        vec4 ryo = texture(RyoSampler, fract(atlasPixels / VANILLA_SPRITE_SIZE));
        float overlayStrength = RYO_OVERLAY_STRENGTH * ryo.a;
        color = vec4(mix(vanilla.rgb, ryo.rgb, overlayStrength), vanilla.a) * lightColor;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
