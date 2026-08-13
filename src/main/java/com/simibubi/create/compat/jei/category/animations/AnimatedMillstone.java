package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class AnimatedMillstone extends AnimatedKinetics {
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		AllGuiTextures.JEI_SHADOW.render(graphics, xOffset - 16, yOffset + 13);
		int x = xOffset - 2, y = yOffset + 18, scale = 22;
		blockElement(AllPartialModels.MILLSTONE_COG).at(x, y).rotateBlock(22.5, getCurrentAngle() * 2, 0).scale(scale).submit(graphics);
		blockElement(AllBlocks.MILLSTONE.getDefaultState()).at(x, y).rotateBlock(22.5, 22.5, 0).scale(scale).submit(graphics);
	}

}
