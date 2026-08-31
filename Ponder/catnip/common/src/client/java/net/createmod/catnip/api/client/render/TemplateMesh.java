package net.createmod.catnip.api.client.render;

import net.minecraft.util.ARGB;

public class TemplateMesh {
	public static final int INT_STRIDE = 7;
	public static final int BYTE_STRIDE = INT_STRIDE * Integer.BYTES;

	public static final int X_OFFSET = 0;
	public static final int Y_OFFSET = 1;
	public static final int Z_OFFSET = 2;
	public static final int COLOR_OFFSET = 3;
	public static final int U_OFFSET = 4;
	public static final int V_OFFSET = 5;
	public static final int LIGHT_OFFSET = 6;

	protected int[] data;
	protected int vertexCount;

	public TemplateMesh(int[] data) {
		if (data.length % INT_STRIDE != 0) {
			throw new IllegalArgumentException("Received invalid vertex data");
		}

		this.data = data;
		vertexCount = data.length / INT_STRIDE;
	}

	public TemplateMesh(int vertexCount) {
		data = new int[vertexCount * INT_STRIDE];
		this.vertexCount = vertexCount;
	}

	public float x(int index) {
		return Float.intBitsToFloat(data[index * INT_STRIDE + X_OFFSET]);
	}

	public float y(int index) {
		return Float.intBitsToFloat(data[index * INT_STRIDE + Y_OFFSET]);
	}

	public float z(int index) {
		return Float.intBitsToFloat(data[index * INT_STRIDE + Z_OFFSET]);
	}

	// 0xAABBGGRR, needs to be converted to 0xAARRGGBB
	public int color(int index) {
		// FIXME: why is this ABGR?
		int abgr = data[index * INT_STRIDE + COLOR_OFFSET];
		return ARGB.color(
			ARGB.alpha(abgr),
			ARGB.blue(abgr),
			ARGB.green(abgr),
			ARGB.red(abgr)
		);
	}

	public float u(int index) {
		return Float.intBitsToFloat(data[index * INT_STRIDE + U_OFFSET]);
	}

	public float v(int index) {
		return Float.intBitsToFloat(data[index * INT_STRIDE + V_OFFSET]);
	}

	public int light(int index) {
		return data[index * INT_STRIDE + LIGHT_OFFSET];
	}

	public int vertexCount() {
		return vertexCount;
	}

	public boolean isEmpty() {
		return vertexCount == 0;
	}
}
