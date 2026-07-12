#version 330 compatibility

out vec2 atlasCoord;
out vec2 lightmapCoord;
out vec4 vertexColor;

void main() {
    gl_Position = ftransform();
    atlasCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lightmapCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    vertexColor = gl_Color;
}
