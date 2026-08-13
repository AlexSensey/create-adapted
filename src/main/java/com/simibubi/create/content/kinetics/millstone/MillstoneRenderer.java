package com.simibubi.create.content.kinetics.millstone;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class MillstoneRenderer extends KineticBlockEntityRenderer<MillstoneBlockEntity> {

	private List<BlockStateModelPart> cogModel;

	public MillstoneRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected List<BlockStateModelPart> getRotatingModelParts(MillstoneBlockEntity be, BlockState renderedState) {
		if (cogModel != null)
			return cogModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.MILLSTONE_COG);
		return cogModel = model == null ? List.of() : List.of(model);
	}

}
