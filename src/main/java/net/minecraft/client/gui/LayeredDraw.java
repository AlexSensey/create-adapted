package net.minecraft.client.gui;

import net.minecraft.client.DeltaTracker;

public class LayeredDraw {
	@FunctionalInterface
	public interface Layer {
		void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker);
	}
}
