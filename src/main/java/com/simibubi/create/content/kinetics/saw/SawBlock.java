package com.simibubi.create.content.kinetics.saw;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;

import net.createmod.catnip.api.placement.IPlacementHelper;
import net.createmod.catnip.api.placement.PlacementHelpers;
import net.createmod.catnip.api.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SawBlock extends DirectionalAxisKineticBlock implements IBE<SawBlockEntity> {
	public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");

	private static final IPlacementHelper placementHelper = PlacementHelpers.register(new PlacementHelper());

	public SawBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FLIPPED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState stateForPlacement = super.getStateForPlacement(context);
		Direction facing = stateForPlacement.getValue(FACING);
		return stateForPlacement.setValue(FLIPPED, facing.getAxis() == Axis.Y && context.getHorizontalDirection()
			.getAxisDirection() == AxisDirection.POSITIVE);
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		BlockState newState = super.getRotatedBlockState(originalState, targetedFace);
		if (newState.getValue(FACING)
			.getAxis() != Axis.Y)
			return newState;
		if (targetedFace.getAxis() != Axis.Y)
			return newState;
		if (!originalState.getValue(AXIS_ALONG_FIRST_COORDINATE))
			newState = newState.cycle(FLIPPED);
		return newState;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		BlockState newState = super.rotate(state, rot);
		if (state.getValue(FACING)
			.getAxis() != Axis.Y)
			return newState;

		if (rot.ordinal() % 2 == 1 && (rot == Rotation.CLOCKWISE_90) != state.getValue(AXIS_ALONG_FIRST_COORDINATE))
			newState = newState.cycle(FLIPPED);
		if (rot == Rotation.CLOCKWISE_180)
			newState = newState.cycle(FLIPPED);

		return newState;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		BlockState newState = super.mirror(state, mirrorIn);
		if (state.getValue(FACING)
			.getAxis() != Axis.Y)
			return newState;

		boolean alongX = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
		if (alongX && mirrorIn == Mirror.FRONT_BACK)
			newState = newState.cycle(FLIPPED);
		if (!alongX && mirrorIn == Mirror.LEFT_RIGHT)
			newState = newState.cycle(FLIPPED);

		return newState;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
		if (!rotated.canSurvive(level, pos))
			return InteractionResult.PASS;

		KineticBlockEntity.switchToBlockState(level, pos, updateAfterWrenched(rotated, context));
		if (level.getBlockState(pos) != state)
			IWrenchable.playRotateSound(level, pos);

		if (!level.isClientSide())
			withBlockEntityDo(level, pos, SawBlockEntity::resetBreakingAfterWrenched);
		return InteractionResult.SUCCESS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.CASING_12PX.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!player.isShiftKeyDown() && player.mayBuild()) {
			if (placementHelper.matchesItem(stack) && placementHelper.getOffset(player, level, state, pos, hitResult)
				.placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult)
				.consumesAction())
				return InteractionResult.SUCCESS;
		}

		if (player.isSpectator() || !stack.isEmpty())
			return InteractionResult.PASS;
		if (state.getOptionalValue(FACING)
			.orElse(Direction.WEST) != Direction.UP)
			return InteractionResult.PASS;

		return onBlockEntityUseItemOn(level, pos, be -> {
			for (int i = 0; i < be.inventory.getSlots(); i++) {
				ItemStack heldItemStack = be.inventory.getStackInSlot(i);
				if (!level.isClientSide() && !heldItemStack.isEmpty())
					player.getInventory()
						.placeItemBackInInventory(heldItemStack);
			}
			be.inventory.clear();
			be.notifyUpdate();
			return net.minecraft.world.ItemInteractionResult.SUCCESS;
		}) == net.minecraft.world.ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	@Override
	protected void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn,
								InsideBlockEffectApplier effectApplier, boolean insideBlock) {
		entityInside(state, worldIn, pos, entityIn);
	}

	public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn) {
		if (entityIn instanceof ItemEntity)
			return;
		if (!new AABB(pos).deflate(.1f)
			.intersects(entityIn.getBoundingBox()))
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			if (be.getSpeed() == 0)
				return;
			entityIn.hurt(CreateDamageSources.saw(worldIn), (float) DrillBlock.getDamage(be.getSpeed()));
		});
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		updateEntityAfterFallOn(level, entity);
	}

	public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
		if (!(entityIn instanceof ItemEntity))
			return;
		if (entityIn.level().isClientSide())
			return;

		BlockPos pos = entityIn.blockPosition();
		withBlockEntityDo(entityIn.level(), pos, be -> {
			if (be.getSpeed() == 0)
				return;
			be.insertItem((ItemEntity) entityIn);
		});
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.NORMAL;
	}

	public static boolean isHorizontal(BlockState state) {
		return state.getValue(FACING)
			.getAxis()
			.isHorizontal();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return isHorizontal(state) ? state.getValue(FACING)
			.getAxis() : super.getRotationAxis(state);
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return isHorizontal(state) ? face == state.getValue(FACING)
			.getOpposite() : super.hasShaftTowards(world, pos, state, face);
	}

	@Override
	protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		if (oldState.getBlock() != newState.getBlock())
			return false;
		return oldState.getValue(FACING) == newState.getValue(FACING)
			&& oldState.getValue(AXIS_ALONG_FIRST_COORDINATE) == newState.getValue(AXIS_ALONG_FIRST_COORDINATE)
			&& oldState.getValue(FLIPPED) == newState.getValue(FLIPPED);
	}

	@Override
	public Class<SawBlockEntity> getBlockEntityClass() {
		return SawBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SawBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SAW.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.MECHANICAL_SAW::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return state -> AllBlocks.MECHANICAL_SAW.has(state);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
				state.getValue(FACING)
					.getAxis(),
				dir -> world.getBlockState(pos.relative(dir))
					.canBeReplaced());

			if (directions.isEmpty())
				return PlacementOffset.fail();
			else {
				return PlacementOffset.success(pos.relative(directions.get(0)),
					s -> s.setValue(FACING, state.getValue(FACING))
					.setValue(AXIS_ALONG_FIRST_COORDINATE, state.getValue(AXIS_ALONG_FIRST_COORDINATE))
					.setValue(FLIPPED, state.getValue(FLIPPED)));
			}
		}

	}

}
