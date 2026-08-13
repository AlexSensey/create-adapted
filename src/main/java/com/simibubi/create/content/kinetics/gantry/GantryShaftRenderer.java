package com.simibubi.create.content.kinetics.gantry;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class GantryShaftRenderer extends KineticBlockEntityRenderer<GantryShaftBlockEntity> {

	public GantryShaftRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected BlockState getRenderedBlockState(GantryShaftBlockEntity be) {
		return be.getBlockState();
	}
}
