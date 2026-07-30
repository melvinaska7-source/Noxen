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

float warp(vec3 p, float t, int iterations) {
    vec3 q = p;
    float sum = 0.0;
    float weight = 1.0;
    for (int n = 0; n < iterations; n++) {
        float phase = t * (0.6 + 0.4 * float(n));
        q = vec3(
            q.x + sin(q.z * 1.3 + phase) * 0.6,
            q.y + cos(q.x * 1.1 - phase) * 0.6,
            q.z + sin(q.y * 1.4 + phase * 0.8) * 0.6
        );
        sum += weight / (1.0 + abs(dot(q, p)) * uIntensity * 40.0);
        weight *= 0.55;
        p *= 1.35;
    }
    return sum;
}

void main() {
    vec2 uv = (gl_FragCoord.xy / uResolution.xy) * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);

    float halfFov = radians(uFov) * 0.5;
    float tanHalf = tan(halfFov);
    vec3 rayView = normalize(vec3(uv.x * tanHalf * aspect, uv.y * tanHalf, 1.0));
    vec3 ray = rotateY(uCameraDir.x) * rotateX(uCameraDir.y) * rayView;

    float t = uTime * uSpeed;
    vec3 p = ray * uScale;

    int iterations = uMode > 0.5 ? 6 : 4;
    float field = warp(p, t, iterations);
    field /= float(iterations);

    float glow = pow(clamp(field, 0.0, 1.0), uMode > 0.5 ? 3.0 : 4.5);

    vec3 baseTint = uMode > 0.5 ? mix(uColor, vec3(1.0), 0.25) : uColor;
    vec3 color = baseTint * glow + baseTint * 0.12;

    fragColor = vec4(color, glow * uAlpha + 0.02 * uAlpha);
}
