package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

public class AnimatedSaw extends AnimatedKinetics {
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		int x = xOffset + 2, y = yOffset + 22, scale = 25;
		blockElement(shaft(Direction.Axis.X)).at(x, y).rotateBlock(-getCurrentAngle(), 0, 0).scale(scale).submit(graphics);
		blockElement(AllBlocks.MECHANICAL_SAW.getDefaultState().setValue(SawBlock.FACING, Direction.UP)).at(x, y).scale(scale).submit(graphics);
		blockElement(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE).at(x, y).rotateBlock(0, -90, -90).scale(scale).submit(graphics);
	}

	@Override
	protected float getGlobalXRotation() {
		return -15.5f;
	}

	@Override
	protected float getGlobalYRotation() {
		return 112.5f;
	}

}
