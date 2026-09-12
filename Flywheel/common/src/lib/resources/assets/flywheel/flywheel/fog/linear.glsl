vec4 linearFog(vec4 color, float distance, float fogStart, float fogEnd, vec4 fogColor) {
    if (distance <= fogStart) {
        return color;
    }

    float fogValue = distance < fogEnd ? (distance - fogStart) / (fogEnd - fogStart) : 1.0;
    return vec4(mix(color.rgb, fogColor.rgb, fogValue * fogColor.a), color.a);
}

vec4 flw_fogFilter(vec4 color) {
    return vec4(mix(color.rgb, flw_fogColor.rgb, max(_flw_environmentalFogFactor(), _flw_renderDistanceFogFactor()) * flw_fogColor.a), color.a);
}
