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

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const vec3 FRESH_COPPER = vec3(0.753, 0.424, 0.314);

struct CopperVisualProfile {
    vec3 stageColor;
    float copperUndertone;
    float textureRetention;
    float saturation;
    float valueScale;
};

CopperVisualProfile profileForStage(float oxidation) {
    if (oxidation < 0.17) {
        return CopperVisualProfile(FRESH_COPPER, 0.00, 0.62, 1.08, 1.04);
    } else if (oxidation < 0.50) {
        return CopperVisualProfile(vec3(0.631, 0.494, 0.408), 0.18, 0.60, 0.88, 1.08);
    } else if (oxidation < 0.84) {
        return CopperVisualProfile(vec3(0.424, 0.600, 0.431), 0.30, 0.56, 0.92, 1.03);
    }
    return CopperVisualProfile(vec3(0.322, 0.639, 0.522), 0.10, 0.54, 1.02, 1.06);
}

vec3 preserveTextureDetail(vec3 baseColor, vec3 tintColor, float luminance) {
    float tintLuminance = max(dot(tintColor, LUMA_WEIGHTS), 0.001);
    vec3 neutralTint = tintColor / tintLuminance;
    vec3 multiplied = baseColor * mix(vec3(1.0), neutralTint, 0.72);
    float multipliedLuminance = max(dot(multiplied, LUMA_WEIGHTS), 0.025);
    return multiplied * mix(1.0, luminance / multipliedLuminance, 0.72);
}

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

    CopperVisualProfile profile = profileForStage(copperOxidation);
    float localCopper = profile.copperUndertone * (0.35 + 0.65 * (1.0 - noise));
    vec3 copperBase = mix(profile.stageColor, FRESH_COPPER, localCopper);
    float luminance = dot(base.rgb, LUMA_WEIGHTS);
    vec3 multipliedDetail = preserveTextureDetail(base.rgb, copperBase, luminance);
    vec3 tonalCopper = copperBase * (0.36 + luminance * 1.08);
    tonalCopper += vec3(0.055, 0.022, 0.006) * pow(luminance, 3.0);
    vec3 convertedCopper = mix(tonalCopper, multipliedDetail, profile.textureRetention);
    float convertedLuminance = dot(convertedCopper, LUMA_WEIGHTS);
    convertedCopper = mix(vec3(convertedLuminance), convertedCopper, profile.saturation) * profile.valueScale;
    convertedCopper = clamp(convertedCopper, 0.0, 1.0);

    // Waxing freezes the server-side stage; it intentionally does not change this palette.

    vec4 color = vec4(mix(base.rgb, convertedCopper, copperMask), base.a);
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    vec4 faceLight = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
    color *= faceLight * lightMapColor * ColorModulator;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
