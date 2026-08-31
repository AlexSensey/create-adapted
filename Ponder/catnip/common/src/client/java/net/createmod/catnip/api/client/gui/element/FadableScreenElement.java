package net.createmod.catnip.api.client.gui.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
public interface FadableScreenElement extends ScreenElement {
	@Override
	default void render(GuiGraphicsExtractor graphics, int x, int y) {
		render(graphics, x, y, 1f);
	}

	void render(GuiGraphicsExtractor graphics, int x, int y, float alpha);
}
