package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class AnimatedMixer extends AnimatedKinetics {
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		int scale = 23;
		blockElement(cogwheel()).at(xOffset, yOffset).rotateBlock(0, getCurrentAngle() * 2, 0).scale(scale).submit(graphics);
		blockElement(AllBlocks.MECHANICAL_MIXER.getDefaultState()).at(xOffset, yOffset).scale(scale).submit(graphics);
		float animation = ((Mth.sin(getAnimationTime() / 32f) + 1) / 5) + .5f;
		blockElement(AllPartialModels.MECHANICAL_MIXER_POLE).at(xOffset, yOffset).atLocal(0, animation, 0).scale(scale).submit(graphics);
		blockElement(AllPartialModels.MECHANICAL_MIXER_HEAD).at(xOffset, yOffset).atLocal(0, animation, 0).rotateBlock(0, getCurrentAngle() * 4, 0).scale(scale).submit(graphics);
		blockElement(AllBlocks.BASIN.getDefaultState()).at(xOffset, yOffset).atLocal(0, 1.65f, 0).scale(scale).submit(graphics);
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
