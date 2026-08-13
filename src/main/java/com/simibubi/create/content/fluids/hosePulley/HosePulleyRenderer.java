package com.simibubi.create.content.fluids.hosePulley;

import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction.Axis;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class HosePulleyRenderer extends AbstractPulleyRenderer<HosePulleyBlockEntity> {

	public HosePulleyRenderer(BlockEntityRendererProvider.Context context) {
		super(context, CreateStandaloneModels.HOSE_HALF, CreateStandaloneModels.HOSE_HALF_MAGNET);
	}

	@Override
	protected Axis getShaftAxis(HosePulleyBlockEntity be) {
		return be.getBlockState()
			.getValue(HosePulleyBlock.HORIZONTAL_FACING)
			.getClockWise()
			.getAxis();
	}

	@Override
	protected StandaloneModelKey<BlockStateModelPart> getCoil() {
		return CreateStandaloneModels.HOSE_COIL;
	}

	@Override
	protected StandaloneModelKey<BlockStateModelPart> getRope() {
		return CreateStandaloneModels.HOSE;
	}

	@Override
	protected StandaloneModelKey<BlockStateModelPart> getMagnet() {
		return CreateStandaloneModels.HOSE_MAGNET;
	}

	@Override
	protected float getOffset(HosePulleyBlockEntity be, float partialTicks) {
		return be.getInterpolatedOffset(partialTicks);
	}

	@Override
	protected boolean isRunning(HosePulleyBlockEntity be) {
		return true;
	}

}
