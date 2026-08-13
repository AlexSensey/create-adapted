package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.FluidState;

public abstract class WaterloggedCopycatBlock extends CopycatBlock implements ProperWaterloggedBlock {

	public WaterloggedCopycatBlock(Properties pProperties) {
		super(pProperties);
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(WATERLOGGED));
	}

	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return withWater(super.getStateForPlacement(pContext), pContext);
	}
	
	public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState,
		LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
		net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
		BlockState neighbourState, net.minecraft.util.RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
		if (!updated.is(this))
			return updated;
		if (updated.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
			&& updated.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
			ticks.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER,
				net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
		return updated;
	}

}
