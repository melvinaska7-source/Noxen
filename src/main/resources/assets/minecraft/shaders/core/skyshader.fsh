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

mat2 rot(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float hash(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        v += a * noise(p);
        p = rot(0.5) * p * 2.1;
        a *= 0.5;
    }
    return v;
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
}

// Режим "Волны" — плавный органический fbm-паттерн
vec4 wavesMode(vec3 ray) {
    float t = uTime * uSpeed * 0.3;
    vec2 p = ray.xz / (ray.y + 1.5) * uScale;

    float q = fbm(p + t * 0.5);
    float r = fbm(p + q + vec2(t * 0.7, t * 0.4));
    float field = fbm(p + vec2(r, q) + vec2(t * 0.3, t * 0.6));

    float brightness = pow(clamp(field * 2.0 - 0.3, 0.0, 1.0), 1.8);
    float glow = clamp(pow(brightness, 0.5) * uIntensity * 100.0, 0.0, 1.0);

    vec3 col = uColor * brightness * 1.5;
    col += vec3(1.0) * glow * 0.6;
    col = clamp(col, 0.0, 1.0);

    float alpha = clamp(brightness * 1.2, 0.0, 1.0) * uAlpha;
    return vec4(col, alpha);
}

// Режим "Огонь" — турбулентный reciprocal-length паттерн
vec4 fireMode(vec3 ray) {
    float t = uTime * uSpeed * 10.0;
    vec3 p = ray * uScale;
    vec3 q = p;

    float field = 0.0;
    int iterations = 5;

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

    vec3 tint = uColor;
    vec3 col = tint * brightness + tint * 0.15;

    return vec4(col, brightness * uAlpha + 0.05 * uAlpha);
}

void main() {
    vec2 uv = (gl_FragCoord.xy / uResolution.xy) * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);

    float halfFov = radians(uFov) * 0.5;
    float tanH = tan(halfFov);
    vec3 ray = normalize(vec3(uv.x * tanH * aspect, uv.y * tanH, 1.0));
    ray = rotY(uCameraDir.x) * rotX(uCameraDir.y) * ray;

    if (uMode > 0.5) {
        fragColor = fireMode(ray);
    } else {
        fragColor = wavesMode(ray);
    }
}
