float linearize_depth(float d, float zNear, float zFar) {
    // Minecraft 26.2 builds its projection with zFar and zNear swapped (reversed Z).
    // Deriving eye depth from the actual inverse projection also handles both OpenGL
    // clip-space conventions instead of assuming the old -1..1, forward-Z matrix.
    float ndcDepth = _flw_cullData.zZeroToOne != 0u ? d : d * 2.0 - 1.0;
    vec4 viewPosition = flw_projectionInverse * vec4(0.0, 0.0, ndcDepth, 1.0);
    return -viewPosition.z / viewPosition.w;
}

float delinearize_depth(float linearDepth, float zNear, float zFar) {
    vec4 clipPosition = flw_projection * vec4(0.0, 0.0, -linearDepth, 1.0);
    float ndcDepth = clipPosition.z / clipPosition.w;
    return _flw_cullData.zZeroToOne != 0u ? ndcDepth : ndcDepth * 0.5 + 0.5;
}
