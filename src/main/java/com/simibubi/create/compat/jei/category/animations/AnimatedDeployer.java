package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

public class AnimatedDeployer extends AnimatedKinetics {
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		int scale = 20;
		blockElement(shaft(Direction.Axis.Z)).at(xOffset, yOffset).rotateBlock(0, 0, getCurrentAngle()).scale(scale).submit(graphics);
		blockElement(AllBlocks.DEPLOYER.getDefaultState().setValue(DeployerBlock.FACING, Direction.DOWN)
			.setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, false)).at(xOffset, yOffset).scale(scale).submit(graphics);
		float cycle = (getAnimationTime() - offset * 8) % 30;
		float extension = cycle < 10 ? cycle / 10f : cycle < 20 ? (20 - cycle) / 10f : 0;
		blockElement(AllPartialModels.DEPLOYER_POLE).at(xOffset, yOffset).atLocal(0, extension * .85f, 0)
			.rotateBlock(90, 0, 0).scale(scale).submit(graphics);
		blockElement(AllPartialModels.DEPLOYER_HAND_HOLDING).at(xOffset, yOffset).atLocal(0, extension * .85f, 0)
			.rotateBlock(90, 0, 0).scale(scale).submit(graphics);
		blockElement(AllBlocks.DEPOT.getDefaultState()).at(xOffset, yOffset).atLocal(0, 2, 0)
			.scale(scale).submit(graphics);
	}

	@Override
	protected float getGlobalXRotation() {
		return -15.5f;
	}

	@Override
	protected float getGlobalYRotation() {
		return 22.5f;
	}

}
