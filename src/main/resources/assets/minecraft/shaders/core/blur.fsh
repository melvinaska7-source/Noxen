#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float thickness;
uniform float softness;
uniform float distortion;
uniform float shine;

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform float Quality;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 outlineColor;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    radius.x  = (center.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(center) - size + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

vec4 blur(vec2 uvOffset) {
    #define TAU 6.28318530718
    vec4 rectColor = createGradient((gl_FragCoord.xy - location) / size, color1, color2, color3, color4);
    vec2 Radius = Quality / InputResolution.xy;
    vec2 uv = clamp(gl_FragCoord.xy / InputResolution.xy + uvOffset, vec2(0.0), vec2(1.0));
    vec4 blur = texture(InputSampler, uv);

    float step = TAU / 16;

    for (float d = 0.0; d < TAU; d += step) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            blur += texture(InputSampler, clamp(uv + vec2(cos(d), sin(d)) * Radius * i, vec2(0.0), vec2(1.0)));
        }
    }

    blur /= 80;
    return vec4((blur * (1 - rectColor.a)).rgb, rectColor.a) + rectColor;
}

void main() {
    vec2 center = gl_FragCoord.xy - location - (size / 2.0);
    float distance = roundedBoxSDF(center, size / 2.0, radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., distance);

    // "Liquid" refraction: near the rounded rim, bend the sampled background a
    // little outward/inward like light bending through curved glass. Fades to
    // nothing toward the middle of the panel. distortion == 0 -> no effect at all,
    // so every non-glass caller is unaffected.
    vec2 uvOffset = vec2(0.0);
    if (distortion > 0.0) {
        float edgeFalloff = 1.0 - smoothstep(0.0, max(size.x, size.y) * 0.5, abs(distance));
        vec2 dir = normalize(center + vec2(0.0001));
        uvOffset = dir * edgeFalloff * distortion / InputResolution;
    }

    if(smoothedAlpha < 0.49 && thickness > 0.) {
        float smoothedborderAlpha = (1.0 - smoothstep(-softness,  softness, distance));
        fragColor = vec4(outlineColor.rgb, smoothedborderAlpha * outlineColor.a);
    } else {
        float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(distance));
        vec4 blurred = blur(uvOffset);

        // Rim light: a soft highlight along the top-left edge, like real glass
        // catching light. shine == 0 -> no effect, unaffected non-glass callers.
        if (shine > 0.0) {
            vec2 normal = normalize(vec2(dFdx(distance), dFdy(distance)) + vec2(0.0001));
            float topLight = clamp(dot(normal, normalize(vec2(-0.6, -0.9))), 0.0, 1.0);
            float rim = (1.0 - smoothstep(0.0, 1.5, abs(distance))) * topLight;
            blurred.rgb += vec3(1.0) * rim * shine * 0.25;
        }

        vec4 basicColor = vec4(blurred.rgb, blurred.a * smoothedAlpha);
        fragColor = mix(vec4(blurred.rgb, 0.), mix(basicColor, thickness > 0. ? outlineColor : basicColor, borderAlpha), smoothedAlpha);
    }
}
