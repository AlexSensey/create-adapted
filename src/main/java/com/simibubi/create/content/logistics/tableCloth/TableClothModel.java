package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelData.Builder;

public class TableClothModel extends BakedModelWrapperWithData {

	public TableClothModel(BakedModel originalModel) {
		super(originalModel);
	}

	public static void reload() {
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	protected Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
		ModelData blockEntityData) {
		return builder;
	}

}
