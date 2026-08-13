package com.mojang.blaze3d.vertex;

public class Tesselator {
	private static final Tesselator INSTANCE = new Tesselator();

	public static Tesselator getInstance() {
		return INSTANCE;
	}

	public BufferBuilder begin(Object mode, VertexFormat format) {
		return null;
	}
}
