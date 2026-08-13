package com.simibubi.create.content.fluids.pipes.valve;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public class FluidValveBlock extends DirectionalAxisKineticBlock
	implements IAxisPipe, IBE<FluidValveBlockEntity>, ProperWaterloggedBlock {

	public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

	public FluidValveBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ENABLED, false)
			.setValue(WATERLOGGED, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_,
		CollisionContext p_220053_4_) {
		return AllShapes.FLUID_VALVE.get(getPipeAxis(state));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ENABLED, WATERLOGGED));
	}

	@Override
	protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing, boolean shaftAxis) {
		if (!shaftAxis) {
			BlockPos offset = pos.relative(facing);
			BlockState blockState = reader.getBlockState(offset);
			return FluidPipeBlock.canConnectTo(reader, offset, blockState, facing);
		}
		return super.prefersConnectionTo(reader, pos, facing, shaftAxis);
	}

	@NotNull
	public static Axis getPipeAxis(BlockState state) {
		if (!(state.getBlock() instanceof FluidValveBlock))
			throw new IllegalStateException("Provided BlockState is for a different block.");
		Direction facing = state.getValue(FACING);
		boolean alongFirst = !state.getValue(AXIS_ALONG_FIRST_COORDINATE);
		for (Axis axis : Iterate.axes) {
			if (axis == facing.getAxis())
				continue;
			if (!alongFirst) {
				alongFirst = true;
				continue;
			}
			return axis;
		}
		throw new IllegalStateException("Impossible axis.");
	}

	@Override
	public Axis getAxis(BlockState state) {
		return getPipeAxis(state);
	}

	@Override
	public boolean canSurvive(BlockState p_196260_1_, LevelReader p_196260_2_, BlockPos p_196260_3_) {
		return true;
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, world, pos, oldState, isMoving);
		if (world.isClientSide())
			return;
		if (state != oldState)
			world.scheduleTick(pos, this, 1, TickPriority.HIGH);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean isMoving) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
		super.affectNeighborsAfterRemoval(state, world, pos, isMoving);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block otherBlock, Orientation orientation,
		boolean isMoving) {
		if (!world.isClientSide())
			world.scheduleTick(pos, this, 1, TickPriority.HIGH);
	}

	public static boolean isOpenAt(BlockState state, Direction d) {
		return d.getAxis() == getPipeAxis(state);
	}

	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<FluidValveBlockEntity> getBlockEntityClass() {
		return FluidValveBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FluidValveBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.FLUID_VALVE.get();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withWater(super.getStateForPlacement(context), context);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess scheduledTickAccess,
		BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED))
			scheduledTickAccess.scheduleTick(pos, getFluidState(state).getType(), getFluidState(state).getType().getTickDelay(world));
		if (isOpenAt(state, direction))
			scheduledTickAccess.scheduleTick(pos, this, 1, TickPriority.HIGH);
		return state;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return fluidState(state);
	}

}
