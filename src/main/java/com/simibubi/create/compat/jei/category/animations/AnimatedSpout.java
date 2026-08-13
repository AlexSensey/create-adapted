package com.simibubi.create.compat.jei.category.animations;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.TypedInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;

public class AnimatedSpout extends AnimatedKinetics {

	private List<FluidStack> fluids;

	public AnimatedSpout withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
		return this;
	}
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		int scale = 20;
		blockElement(AllBlocks.SPOUT.getDefaultState()).at(xOffset, yOffset).scale(scale).submit(graphics);
		float cycle = (getAnimationTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
		float squeezedPixels = squeeze * 20;
		// atLocal() is scaled together with the model. The old renderer translated
		// the parent PoseStack before applying the model's scale of 20.
		float displacement = -3 * squeeze / 32f;
		blockElement(AllPartialModels.SPOUT_TOP).at(xOffset, yOffset).scale(scale).submit(graphics);
		blockElement(AllPartialModels.SPOUT_MIDDLE).at(xOffset, yOffset).atLocal(0, displacement, 0).scale(scale).submit(graphics);
		blockElement(AllPartialModels.SPOUT_BOTTOM).at(xOffset, yOffset).atLocal(0, displacement * 2, 0).scale(scale).submit(graphics);
		blockElement(AllBlocks.DEPOT.getDefaultState()).at(xOffset, yOffset).atLocal(0, 2, 0).scale(scale).submit(graphics);

		if (fluids == null || fluids.isEmpty())
			return;
		@SuppressWarnings("unchecked")
		TypedInstance<Fluid> fluid = (TypedInstance<Fluid>) fluids.getFirst();
		float fluidScale = 16f / scale;
		float from = 3f / 16f;
		float to = 17f / 16f;
		GuiGameElement.submitFluidBox(fluid, 0, 0, 0, fluidScale,
			0, 0, 0, from, from, from, to, to, to);

		float width = squeezedPixels / 128f;
		float streamFrom = .5f - width / 2;
		float streamTo = .5f + width / 2;
		GuiGameElement.submitFluidBox(fluid, .5f, 1.5f, .5f, fluidScale,
			-.5f, 0, -.5f, streamFrom, 0, streamFrom, streamTo, 2, streamTo);
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
