package com.simibubi.create.foundation.block.connected;

import java.util.Arrays;
import java.util.List;

import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.CTContext;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelData.Builder;
import net.neoforged.neoforge.model.data.ModelProperty;

public class CTModel extends BakedModelWrapperWithData {

	private static final ModelProperty<CTData> CT_PROPERTY = new ModelProperty<>();

	private final ConnectedTextureBehaviour behaviour;

	public CTModel(BakedModel originalModel, ConnectedTextureBehaviour behaviour) {
		super(originalModel);
		this.behaviour = behaviour;
	}

	@Override
	protected ModelData.Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
		ModelData blockEntityData) {
		return builder.with(CT_PROPERTY, createCTData(world, pos, state));
	}

	protected CTData createCTData(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		return new CTData();
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
		return super.getQuads(state, side, rand, extraData, renderType);
	}

	private static class CTData {
		private final int[] indices;

		public CTData() {
			indices = new int[6];
			Arrays.fill(indices, -1);
		}

		public void put(Direction face, int texture) {
			indices[face.get3DDataValue()] = texture;
		}

		public int get(Direction face) {
			return indices[face.get3DDataValue()];
		}
	}

}
