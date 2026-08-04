#version 150

layout(std140) uniform NoxenGlassData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uScreen;
    vec4 uParams;
    vec4 uTint;
    vec4 uState;
};

out vec2 texCoord;
out vec2 localPos;
out vec2 rectSize;

void main() {
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[gl_VertexID]];

    localPos = vertex * uRect.zw;
    rectSize = uRect.zw;

    vec2 pos = uRect.xy + vertex * uRect.zw;
    gl_Position = uProjection * vec4(pos, uState.y, 1.0);
    texCoord = vec2(pos.x / uScreen.x, 1.0 - pos.y / uScreen.y);
}
