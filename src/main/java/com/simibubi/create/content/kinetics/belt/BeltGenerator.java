package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class BeltGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		Direction direction = state.getValue(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope = state.getValue(BeltBlock.SLOPE);
		return slope == BeltSlope.VERTICAL ? 90
			: slope == BeltSlope.SIDEWAYS && direction.getAxisDirection() == AxisDirection.NEGATIVE ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		Boolean casing = state.getValue(BeltBlock.CASING);
		BeltSlope slope = state.getValue(BeltBlock.SLOPE);

		boolean flip = slope == BeltSlope.UPWARD;
		boolean rotate = casing && slope == BeltSlope.VERTICAL;
		Direction direction = state.getValue(BeltBlock.HORIZONTAL_FACING);
		return horizontalAngle(direction) + (flip ? 180 : 0) + (rotate ? 90 : 0);
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		BlockState state) {
		BeltPart part = state.getValue(BeltBlock.PART);
		BeltPart modelPart = part == BeltPart.PULLEY ? BeltPart.MIDDLE : part;
		BeltSlope slope = state.getValue(BeltBlock.SLOPE);
		boolean casing = state.getValue(BeltBlock.CASING);

		String partName = modelPart.getSerializedName();
		if (casing) {
			String slopeName = slope.isDiagonal() ? "diagonal" : slope == BeltSlope.SIDEWAYS ? "sideways" : "horizontal";
			return prov.models()
				.getExistingFile(Identifier.fromNamespaceAndPath("create", "block/belt_casing/" + slopeName + "_" + partName));
		}

		if (slope.isDiagonal())
			return prov.models()
				.getExistingFile(Identifier.fromNamespaceAndPath("create", "block/belt/diagonal_" + partName));

		return prov.models()
			.getExistingFile(Identifier.fromNamespaceAndPath("create", "block/belt/" + partName));
	}

}
