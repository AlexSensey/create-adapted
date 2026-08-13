package com.simibubi.create.content.decoration.copycat;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.model.data.ModelData;

public class CopycatPanelModel extends CopycatModel {

	public CopycatPanelModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material,
		ModelData wrappedData, RenderType renderType) {
		return Collections.emptyList();
	}

}
