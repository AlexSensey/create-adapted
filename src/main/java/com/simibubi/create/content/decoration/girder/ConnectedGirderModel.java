package com.simibubi.create.content.decoration.girder;

import java.util.Arrays;
import java.util.List;

import com.simibubi.create.foundation.block.connected.CTModel;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelData.Builder;
import net.neoforged.neoforge.model.data.ModelProperty;

public class ConnectedGirderModel extends CTModel {

	protected static final ModelProperty<ConnectionData> CONNECTION_PROPERTY = new ModelProperty<>();

	public ConnectedGirderModel(BakedModel originalModel) {
		super(originalModel, new GirderCTBehaviour());
	}

	@Override
	protected ModelData.Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
		ModelData blockEntityData) {
		super.gatherModelData(builder, world, pos, state, blockEntityData);
		ConnectionData connectionData = new ConnectionData();
		for (Direction d : Iterate.horizontalDirections)
			connectionData.setConnected(d, GirderBlock.isConnected(world, pos, state, d));
		return builder.with(CONNECTION_PROPERTY, connectionData);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
		return super.getQuads(state, side, rand, extraData, renderType);
	}

	private static class ConnectionData {
		boolean[] connectedFaces;

		public ConnectionData() {
			connectedFaces = new boolean[4];
			Arrays.fill(connectedFaces, false);
		}

		void setConnected(Direction face, boolean connected) {
			connectedFaces[face.get2DDataValue()] = connected;
		}

		boolean isConnected(Direction face) {
			return connectedFaces[face.get2DDataValue()];
		}
	}

}
