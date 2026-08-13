package com.simibubi.create.content.schematics.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class SchematicHotbarSlotOverlay {

	public void renderOn(GuiGraphicsExtractor graphics, int slot, ItemStack schematic) {
		int x = graphics.guiWidth() / 2 - 88;
		int y = graphics.guiHeight() - 19;
		int itemX = x + 20 * slot;
		AllGuiTextures.SCHEMATIC_SLOT.render(graphics, itemX, y);
		graphics.item(schematic, itemX, y);
	}
}
