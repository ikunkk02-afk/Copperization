#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexPerFaceColorBack;
out vec4 vertexPerFaceColorFront;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out float normalizedEntityHeight;
out float copperProgress;
flat out float copperOxidation;
flat out float copperWaxed;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
    vertexPerFaceColorBack = minecraft_mix_light_separate(-light, vec4(1.0));
    vertexPerFaceColorFront = minecraft_mix_light_separate(light, vec4(1.0));
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;

    float encodedY = floor(Color.g * 255.0 + 0.5) * 256.0 + floor(Color.b * 255.0 + 0.5);
    float entityBaseY = encodedY / 256.0 - 128.0;
    float metadata = floor(Color.a * 255.0 + 0.5);
    float heightBucket = floor(metadata / 8.0);
    float entityHeight = max(heightBucket / 31.0 * 4.0, 0.25);
    normalizedEntityHeight = clamp((Position.y - entityBaseY) / entityHeight, 0.0, 1.0);
    copperProgress = Color.r;
    copperOxidation = floor(mod(metadata, 8.0) / 2.0) / 3.0;
    copperWaxed = mod(metadata, 2.0);
}
