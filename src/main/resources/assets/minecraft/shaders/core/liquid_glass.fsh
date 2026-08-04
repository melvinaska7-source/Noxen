#version 150

layout(std140) uniform NoxenGlassData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uScreen;
    vec4 uParams;
    vec4 uTint;
    vec4 uState;
};

in vec2 texCoord;
in vec2 localPos;
in vec2 rectSize;

out vec4 fragColor;

uniform sampler2D Sampler0;

float roundedBoxSDF(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 centerPos = localPos - halfSize;
    float radius = min(uParams.x, min(halfSize.x, halfSize.y));
    float dist = roundedBoxSDF(centerPos, halfSize, radius);
    float smoothing = max(fwidth(dist), 0.75);
    float shapeAlpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (shapeAlpha < 0.01) {
        discard;
    }

    vec2 uv = localPos / rectSize;
    vec2 centered = uv - 0.5;
    float edgeDistance = abs(dist);
    float rim = 1.0 - smoothstep(0.0, max(2.0, radius * 0.45), edgeDistance);

    float waveA = sin((uv.y * 15.0 + uv.x * 5.0) + uState.z * 0.85);
    float waveB = cos((uv.x * 18.0 - uv.y * 4.0) - uState.z * 0.7);
    vec2 bend = normalize(centered + vec2(0.0001)) * (0.35 + rim * 1.65);
    bend += vec2(waveA, waveB) * 0.28;

    float distortionPx = uParams.z * (0.6 + rim * 1.8);
    vec2 distortion = bend * distortionPx / uScreen.xy;
    distortion.y = -distortion.y;

    float blurPx = max(0.0, uParams.y);
    vec2 texel = vec2(1.0 / uScreen.x, 1.0 / uScreen.y);
    vec2 sampleUv = texCoord + distortion;

    vec3 color = texture(Sampler0, sampleUv).rgb * 0.28;
    color += texture(Sampler0, sampleUv + texel * vec2( blurPx, 0.0)).rgb * 0.12;
    color += texture(Sampler0, sampleUv + texel * vec2(-blurPx, 0.0)).rgb * 0.12;
    color += texture(Sampler0, sampleUv + texel * vec2(0.0,  blurPx)).rgb * 0.12;
    color += texture(Sampler0, sampleUv + texel * vec2(0.0, -blurPx)).rgb * 0.12;
    color += texture(Sampler0, sampleUv + texel * vec2( blurPx,  blurPx) * 0.72).rgb * 0.08;
    color += texture(Sampler0, sampleUv + texel * vec2(-blurPx,  blurPx) * 0.72).rgb * 0.08;
    color += texture(Sampler0, sampleUv + texel * vec2( blurPx, -blurPx) * 0.72).rgb * 0.08;
    color += texture(Sampler0, sampleUv + texel * vec2(-blurPx, -blurPx) * 0.72).rgb * 0.08;

    vec3 tint = uTint.rgb;
    color = mix(color, tint, clamp(uTint.a, 0.0, 1.0));

    float alpha = shapeAlpha * clamp(uState.x, 0.0, 1.0);
    fragColor = vec4(color, alpha);
}
