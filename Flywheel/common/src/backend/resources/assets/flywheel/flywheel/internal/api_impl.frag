#include "flywheel:internal/material.glsl"
#include "flywheel:internal/api_impl.glsl"
#include "flywheel:internal/uniforms/uniforms.glsl"

in vec4 flw_vertexPos;
in vec4 flw_vertexColor;
in vec2 flw_vertexTexCoord;
flat in ivec2 flw_vertexOverlay;
in vec2 flw_vertexLight;
in vec3 flw_vertexNormal;

in float flw_distance;
in float flw_cylindricalDistance;

// Match Minecraft 26.2: spherical environment fog, cylindrical distance fog.
float _flw_linearFogFactor(float distance, vec2 range) {
    if (distance <= range.x) return 0.0;
    if (distance >= range.y) return 1.0;
    return (distance - range.x) / (range.y - range.x);
}

float _flw_environmentalFogFactor() {
    return _flw_linearFogFactor(flw_distance, flw_environmentalFogRange);
}

float _flw_renderDistanceFogFactor() {
    return _flw_linearFogFactor(flw_cylindricalDistance, flw_fogRange);
}

vec4 flw_sampleColor;

FlwMaterial flw_material;

bool flw_fragDiffuse;
vec4 flw_fragColor;
ivec2 flw_fragOverlay;
vec2 flw_fragLight;

uniform sampler2D flw_diffuseTex;
uniform sampler2D flw_overlayTex;
uniform sampler2D flw_lightTex;
