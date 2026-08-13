package dev.engine_room.flywheel.lib.model.baked;

import com.mojang.blaze3d.vertex.VertexConsumer;

final class DiscardingVertexConsumer implements VertexConsumer {
	@Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
	@Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
	@Override public VertexConsumer setColor(int color) { return this; }
	@Override public VertexConsumer setUv(float u, float v) { return this; }
	@Override public VertexConsumer setUv1(int u, int v) { return this; }
	@Override public VertexConsumer setUv2(int u, int v) { return this; }
	@Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
	@Override public VertexConsumer setLineWidth(float width) { return this; }
}
