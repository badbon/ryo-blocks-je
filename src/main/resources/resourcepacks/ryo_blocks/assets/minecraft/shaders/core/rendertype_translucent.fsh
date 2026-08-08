#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

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
    vec4 color = atlasSample * vertexColor * ColorModulator * lightColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
