package com.simibubi.create.content.logistics.depot;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepotBlock extends Block implements IBE<DepotBlockEntity>, IWrenchable, ProperWaterloggedBlock {

	public DepotBlock(Properties p_i48440_1_) {
		super(p_i48440_1_);
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(WATERLOGGED));
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
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

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return withWater(super.getStateForPlacement(pContext), pContext);
	}

	@Override
	public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_,
		CollisionContext p_220053_4_) {
		return AllShapes.CASING_13PX.get(Direction.UP);
	}

	@Override
	public Class<DepotBlockEntity> getBlockEntityClass() {
		return DepotBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DepotBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.DEPOT.get();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hitResult) {
		return SharedDepotBlockMethods.onUse(stack, state, level, pos, player, hand, hitResult) == ItemInteractionResult.SUCCESS
			? InteractionResult.SUCCESS
			: InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
		BlockHitResult hitResult) {
		return SharedDepotBlockMethods.onUse(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND,
			hitResult) == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, worldIn, pos, newState);
	}

	public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
		SharedDepotBlockMethods.onLanded(worldIn, entityIn);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		SharedDepotBlockMethods.onLanded(level, entity);
	}

	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos, Direction direction) {
		return SharedDepotBlockMethods.getComparatorInputOverride(blockState, worldIn, pos);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

}
