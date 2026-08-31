package net.createmod.catnip.api.client.render;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface ColoredRenderable {
	void render(GuiGraphicsExtractor graphics, int x, int y, Color c);
}
