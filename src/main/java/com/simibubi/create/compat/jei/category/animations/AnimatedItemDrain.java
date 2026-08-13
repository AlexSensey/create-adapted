package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;

import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;

public class AnimatedItemDrain extends AnimatedKinetics {

	private FluidStack fluid;

	public AnimatedItemDrain withFluid(FluidStack fluid) {
		this.fluid = fluid;
		return this;
	}
	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		blockElement(AllBlocks.ITEM_DRAIN.getDefaultState()).at(xOffset, yOffset)
			.scale(20).submit(graphics);
		if (fluid == null || fluid.isEmpty())
			return;
		@SuppressWarnings("unchecked")
		TypedInstance<Fluid> renderedFluid = (TypedInstance<Fluid>) fluid;
		float from = 2 / 16f;
		GuiGameElement.submitFluidBox(renderedFluid, 0, 0, 0, 1,
			0, 0, 0, from, from, from, 1 - from, 3 / 4f, 1 - from);
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
