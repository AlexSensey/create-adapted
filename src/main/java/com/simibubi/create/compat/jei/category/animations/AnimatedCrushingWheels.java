package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class AnimatedCrushingWheels extends AnimatedKinetics {

	private final BlockState wheel = AllBlocks.CRUSHING_WHEEL.getDefaultState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		int scale = 22;
		blockElement(wheel).at(xOffset, yOffset).rotateBlock(0, 90, -getCurrentAngle()).scale(scale).submit(graphics);
		blockElement(wheel).at(xOffset, yOffset).atLocal(2, 0, 0).rotateBlock(0, 90, getCurrentAngle()).scale(scale).submit(graphics);
	}

	@Override
	protected float getGlobalYRotation() {
		return -22.5f;
	}

}
