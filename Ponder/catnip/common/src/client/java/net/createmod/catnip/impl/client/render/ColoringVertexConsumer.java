package net.createmod.catnip.impl.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.ARGB;

public record ColoringVertexConsumer(VertexConsumer delegate, float red, float green, float blue,
									 float alpha) implements VertexConsumer {
	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		delegate.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		delegate.setColor((int) (r * red), (int) (g * green), (int) (b * blue), (int) (a * alpha));
		return this;
	}

	@Override
	public VertexConsumer setColor(int packedColor) {
		int r = ARGB.red(packedColor);
		int g = ARGB.green(packedColor);
		int b = ARGB.blue(packedColor);
		int a = ARGB.alpha(packedColor);
		delegate.setColor(ARGB.color((int) (a * alpha), (int) (r * red), (int) (g * green), (int) (b * blue)));
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		delegate.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		delegate.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		delegate.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		delegate.setNormal(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		delegate.setLineWidth(width);
		return this;
	}
}
