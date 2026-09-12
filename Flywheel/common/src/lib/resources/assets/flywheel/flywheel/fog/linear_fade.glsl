vec4 linearFogFade(vec4 color, float distance, float fogStart, float fogEnd) {
    if (distance <= fogStart) {
        return color;
    } else if (distance >= fogEnd) {
        return vec4(0.0);
    }

    return color * (1.0 - (distance - fogStart) / (fogEnd - fogStart));
}

vec4 flw_fogFilter(vec4 color) {
    return color * (1.0 - max(_flw_environmentalFogFactor(), _flw_renderDistanceFogFactor()));
}
