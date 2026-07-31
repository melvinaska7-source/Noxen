#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
uniform vec3 Color1;
uniform vec3 Color2;
uniform float Speed;
uniform float Scale;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 5; ++i) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 st = (gl_FragCoord.xy - 0.5 * InSize) / min(InSize.x, InSize.y);
    st *= Scale;
    
    float t = Time * Speed;
    
    vec2 q = vec2(0.0);
    q.x = fbm(st + vec2(0.0, t * 0.1));
    q.y = fbm(st + vec2(1.0, t * 0.1));
    
    vec2 r = vec2(0.0);
    r.x = fbm(st + 1.0 * q + vec2(1.7, 9.2) + 0.15 * t);
    r.y = fbm(st + 1.0 * q + vec2(8.3, 2.8) + 0.126 * t);
    
    float f = fbm(st + r);
    
    vec3 color = mix(Color1, Color2, clamp(f * f * 4.0, 0.0, 1.0));
    color = mix(color, Color2, clamp(length(q), 0.0, 1.0));
    color = mix(color, Color1, clamp(length(r.x), 0.0, 1.0));
    
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    fragColor = vec4(mix(sceneColor.rgb, color, 0.6), sceneColor.a);
}
