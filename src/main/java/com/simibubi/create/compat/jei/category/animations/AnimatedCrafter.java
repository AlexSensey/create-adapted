package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class AnimatedCrafter extends AnimatedKinetics {
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		AllGuiTextures.JEI_SHADOW.render(graphics, xOffset - 16, yOffset + 13);
		int x = xOffset + 3, y = yOffset + 16, scale = 22;
		blockElement(cogwheel()).at(x, y).rotateBlock(90, 0, getCurrentAngle()).scale(scale).submit(graphics);
		blockElement(AllBlocks.MECHANICAL_CRAFTER.getDefaultState()).at(x, y).rotateBlock(0, 180, 0).scale(scale).submit(graphics);
	}

	@Override
	protected float getGlobalXRotation() {
		return -12.5f;
	}

	@Override
	protected float getGlobalYRotation() {
		return -22.5f;
	}

}
