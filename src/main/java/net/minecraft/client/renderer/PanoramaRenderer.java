package net.minecraft.client.renderer;

import net.minecraft.client.gui.GuiGraphics;

public class PanoramaRenderer {
	private final CubeMap cubeMap;

	public PanoramaRenderer(CubeMap cubeMap) {
		this.cubeMap = cubeMap;
	}

	public void render(GuiGraphics graphics, int width, int height, float alpha, float partialTicks) {
	}
}
