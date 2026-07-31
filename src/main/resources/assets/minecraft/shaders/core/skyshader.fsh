#version 150

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
uniform vec2 uCameraDir;
uniform float uFov;
uniform float uMode;

out vec4 fragColor;

mat3 rotateY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

mat3 rotateX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
}

void main() {
    vec2 uv = (gl_FragCoord.xy / uResolution.xy) * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);

    float halfFov = radians(uFov) * 0.5;
    float tanHalf = tan(halfFov);
    vec3 rayView = normalize(vec3(uv.x * tanHalf * aspect, uv.y * tanHalf, 1.0));
    vec3 ray = rotateY(uCameraDir.x) * rotateX(uCameraDir.y) * rayView;

    float t = uTime * uSpeed * (uMode > 0.5 ? 14.0 : 10.0);
    vec3 p = ray * uScale;
    vec3 q = p;

    float field = 0.0;
    int iterations = uMode > 0.5 ? 6 : 5;

    for (int n = 0; n < iterations; n++) {
        float nn = float(n + 1);
        float phase = t * (11.0 - (3.0 / nn));
        q = p + vec3(
            cos(phase - q.x) + sin(phase + q.y),
            sin(phase - q.y) + cos(phase + q.z),
            cos(phase - q.z) + sin(phase + q.x)
        );
        vec3 denom = vec3(
            p.x / (sin(q.x + phase) / uIntensity),
            p.y / (cos(q.y + phase) / uIntensity),
            p.z / (sin(q.z + phase) / uIntensity)
        );
        field += 1.0 / max(length(denom), 0.0001);
    }

    field /= float(iterations);
    field = 1.5 - sqrt(clamp(field, 0.0, 2.25));
    float brightness = clamp(field * field * field * field, 0.0, 1.0);

    vec3 tint = uMode > 0.5 ? mix(uColor, vec3(1.0), 0.2) : uColor;
    vec3 color = tint * brightness + tint * 0.15;

    fragColor = vec4(color, brightness * uAlpha + 0.05 * uAlpha);
}
