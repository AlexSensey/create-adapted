package com.simibubi.create.content.logistics.funnel;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;

import net.createmod.catnip.api.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public abstract class AbstractFunnelBlock extends Block
	implements IBE<FunnelBlockEntity>, IWrenchable, ProperWaterloggedBlock {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	protected AbstractFunnelBlock(Properties p_i48377_1_) {
		super(p_i48377_1_);
		registerDefaultState(defaultBlockState().setValue(POWERED, false)
			.setValue(WATERLOGGED, false));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withWater(defaultBlockState().setValue(POWERED, context.getLevel()
			.hasNeighborSignal(context.getClickedPos())), context);
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	protected BlockState updateShape(BlockState pState, LevelReader pLevel, ScheduledTickAccess scheduledTickAccess,
		BlockPos pCurrentPos, Direction pDirection, BlockPos pNeighborPos, BlockState pNeighborState,
		RandomSource random) {
		if (pLevel instanceof LevelAccessor levelAccessor)
			updateWater(levelAccessor, pState, pCurrentPos);
		return pState;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(POWERED, WATERLOGGED));
	}

	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
		net.minecraft.world.level.redstone.Orientation orientation,
								boolean isMoving) {
		if (level.isClientSide())
			return;
		InvManipulationBehaviour behaviour = BlockEntityBehaviour.get(level, pos, InvManipulationBehaviour.TYPE);
		if (behaviour != null)
			behaviour.onNeighborChanged(behaviour.getTarget().getConnectedPos());
		if (!level.getBlockTicks()
			.willTickThisTick(pos, this))
			level.scheduleTick(pos, this, 1);
	}

	@Override
	public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource r) {
		boolean previouslyPowered = state.getValue(POWERED);
		if (previouslyPowered != worldIn.hasNeighborSignal(pos))
			worldIn.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
	}

	public static ItemStack tryInsert(Level worldIn, BlockPos pos, ItemStack toInsert, boolean simulate) {
		FilteringBehaviour filter = BlockEntityBehaviour.get(worldIn, pos, FilteringBehaviour.TYPE);
		InvManipulationBehaviour inserter = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
		if (inserter == null)
			return toInsert;
		if (filter != null && !filter.test(toInsert))
			return toInsert;
		if (simulate)
			inserter.simulate();
		ItemStack insert = inserter.insert(toInsert);
		if (insert.getCount() == toInsert.getCount())
			insert = tryInsertIntoTargetDepot(worldIn, inserter, toInsert, simulate);

		if (!simulate && insert.getCount() != toInsert.getCount()) {
			BlockEntity blockEntity = worldIn.getBlockEntity(pos);
			if (blockEntity instanceof FunnelBlockEntity funnelBlockEntity) {
				funnelBlockEntity.onTransfer(toInsert);
				if (funnelBlockEntity.hasFlap())
					funnelBlockEntity.flap(true);
			}
		}
		return insert;
	}

	private static ItemStack tryInsertIntoTargetDepot(Level worldIn, InvManipulationBehaviour inserter,
		ItemStack toInsert, boolean simulate) {
		BlockFace target = inserter.getTarget()
			.getOpposite();
		DepotBehaviour depot = BlockEntityBehaviour.get(worldIn, target.getPos(), DepotBehaviour.TYPE);
		if (depot == null)
			return toInsert;
		return depot.itemHandler.insertItem(0, toInsert, simulate);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		Block block = world.getBlockState(pos.relative(getFunnelFacing(state).getOpposite()))
			.getBlock();
		return !(block instanceof AbstractFunnelBlock);
	}

	@Nullable
	public static boolean isFunnel(BlockState state) {
		return state.getBlock() instanceof AbstractFunnelBlock;
	}

	@Nullable
	public static Direction getFunnelFacing(BlockState state) {
		if (!(state.getBlock() instanceof AbstractFunnelBlock))
			return null;
		return ((AbstractFunnelBlock) state.getBlock()).getFacing(state);
	}

	protected abstract Direction getFacing(BlockState state);

	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock() && !isFunnel(newState) || !newState.hasBlockEntity())
			IBE.onRemove(state, world, pos, newState);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
										   BlockPos pos, boolean isMoving) {
		onRemove(state, level, pos, level.getBlockState(pos), isMoving);
		super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
	}

	@Override
	public Class<FunnelBlockEntity> getBlockEntityClass() {
		return FunnelBlockEntity.class;
	}

	public BlockEntityType<? extends FunnelBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.FUNNEL.get();
	}

	;

}
