#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in float normalizedEntityHeight;
in float copperProgress;
flat in float copperOxidation;
flat in float copperWaxed;

out vec4 fragColor;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float stableNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    return mix(
        mix(hash21(cell), hash21(cell + vec2(1.0, 0.0)), local.x),
        mix(hash21(cell + vec2(0.0, 1.0)), hash21(cell + vec2(1.0, 1.0)), local.x),
        local.y
    );
}

float erosionNoise(vec2 uv) {
    float noise = stableNoise(uv * 47.0) * 0.58;
    noise += stableNoise(uv * 103.0 + 13.7) * 0.29;
    noise += stableNoise(uv * 211.0 + 37.1) * 0.13;
    return noise;
}

void main() {
    vec4 base = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (base.a < ALPHA_CUTOUT) discard;
#endif

    float noise = erosionNoise(texCoord0);
    float boundary = clamp(normalizedEntityHeight + (noise - 0.5) * 0.22, 0.0, 1.0);
    float copperMask = smoothstep(boundary - 0.028, boundary + 0.028, copperProgress)
        * smoothstep(0.0, 0.035, copperProgress);
    if (copperProgress >= 0.998) copperMask = 1.0;

    vec3 freshCopper = vec3(0.84, 0.39, 0.20);
    vec3 exposedCopper = vec3(0.71, 0.43, 0.31);
    vec3 weatheredCopper = vec3(0.39, 0.58, 0.46);
    vec3 oxidizedCopper = vec3(0.27, 0.66, 0.53);
    vec3 copperBase;
    if (copperOxidation < 0.17) {
        copperBase = freshCopper;
    } else if (copperOxidation < 0.50) {
        copperBase = exposedCopper;
    } else if (copperOxidation < 0.84) {
        copperBase = weatheredCopper;
    } else {
        copperBase = oxidizedCopper;
    }
    copperBase = mix(copperBase, min(copperBase * 1.10 + vec3(0.035, 0.018, 0.0), vec3(1.0)), copperWaxed);

    float luminance = dot(base.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 convertedCopper = copperBase * (0.34 + luminance * 1.04);
    convertedCopper += vec3(0.085, 0.032, 0.008) * pow(luminance, 3.0);
    convertedCopper = clamp(convertedCopper, 0.0, 1.0);

    vec4 color = vec4(mix(base.rgb, convertedCopper, copperMask), base.a);
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    vec4 faceLight = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
    color *= faceLight * lightMapColor * ColorModulator;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
