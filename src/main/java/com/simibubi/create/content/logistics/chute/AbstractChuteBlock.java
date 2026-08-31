package com.simibubi.create.content.logistics.chute;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractChuteBlock extends Block implements IWrenchable, IBE<ChuteBlockEntity> {

	public AbstractChuteBlock(Properties p_i48440_1_) {
		super(p_i48440_1_);
	}

	public static boolean isChute(BlockState state) {
		return state.getBlock() instanceof AbstractChuteBlock;
	}

	public static boolean isOpenChute(BlockState state) {
		return isChute(state) && ((AbstractChuteBlock) state.getBlock()).isOpen(state);
	}

	public static boolean isTransparentChute(BlockState state) {
		return isChute(state) && ((AbstractChuteBlock) state.getBlock()).isTransparent(state);
	}

	@Nullable
	public static Direction getChuteFacing(BlockState state) {
		return !isChute(state) ? null : ((AbstractChuteBlock) state.getBlock()).getFacing(state);
	}

	public Direction getFacing(BlockState state) {
		return Direction.DOWN;
	}

	public boolean isOpen(BlockState state) {
		return true;
	}

	public boolean isTransparent(BlockState state) {
		return false;
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
		ItemStack stack = ItemHelper.fromItemEntity(entityIn);
		if (stack.isEmpty())
			return;
		if (entityIn.level().isClientSide())
			return;
		if (!entityIn.isAlive())
			return;
		BlockPos pos = BlockPos.containing(entityIn.position()
			.add(0, 0.5f, 0))
			.below();
		DirectBeltInputBehaviour input = BlockEntityBehaviour.get(entityIn.level(), pos, DirectBeltInputBehaviour.TYPE);
		if (input == null)
			return;
		if (!input.canInsertFromSide(Direction.UP))
			return;
		if (!PackageEntity.centerPackage(entityIn, Vec3.atBottomCenterOf(pos.above())))
			return;
		ItemStack remainder = input.handleInsertion(stack, Direction.UP, false);
		if (remainder.isEmpty()) {
			entityIn.discard();
			if (entityIn instanceof PackageEntity box) {
				Player player = box.tossedBy.get();
				if (player != null)
					AllAdvancements.PACKAGE_CHUTE_THROW.awardTo(player);
			}
		} else if (remainder.getCount() < stack.getCount() && entityIn instanceof ItemEntity)
			((ItemEntity) entityIn).setItem(remainder);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		updateEntityAfterFallOn(level, entity);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
		withBlockEntityDo(world, pos, ChuteBlockEntity::onAdded);
		updateDiagonalNeighbour(state, world, pos);
	}

	protected void updateDiagonalNeighbour(BlockState state, Level world, BlockPos pos) {
		if (!isChute(state))
			return;
		AbstractChuteBlock block = (AbstractChuteBlock) state.getBlock();
		Direction facing = block.getFacing(state);
		BlockPos toUpdate = pos.below();
		if (facing.getAxis()
			.isHorizontal())
			toUpdate = toUpdate.relative(facing.getOpposite());

		BlockState stateToUpdate = world.getBlockState(toUpdate);
		if (isChute(stateToUpdate) && !world.getBlockTicks()
			.hasScheduledTick(toUpdate, stateToUpdate.getBlock()))
			world.scheduleTick(toUpdate, stateToUpdate.getBlock(), 1);
	}

	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, world, pos, newState);

		if (state.is(newState.getBlock()))
			return;

		updateDiagonalNeighbour(state, world, pos);

		for (Direction direction : Iterate.horizontalDirections) {
			BlockPos toUpdate = pos.above()
				.relative(direction);
			BlockState stateToUpdate = world.getBlockState(toUpdate);
			if (isChute(stateToUpdate) && !world.getBlockTicks()
				.hasScheduledTick(toUpdate, stateToUpdate.getBlock()))
				world.scheduleTick(toUpdate, stateToUpdate.getBlock(), 1);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
										   BlockPos pos, boolean isMoving) {
		onRemove(state, level, pos, level.getBlockState(pos), isMoving);
		super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
	}

	public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
		BlockState updated = updateChuteState(pState, pLevel.getBlockState(pPos.above()), pLevel, pPos);
		if (pState != updated)
			pLevel.setBlockAndUpdate(pPos, updated);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
		BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState above, RandomSource random) {
		if (direction != Direction.UP)
			return state;
		return updateChuteState(state, above, level, pos);
	}

	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbourBlock,
		net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
		withBlockEntityDo(world, pos, ChuteBlockEntity::blockBelowChanged);
	}

	public abstract BlockState updateChuteState(BlockState state, BlockState above, BlockGetter world, BlockPos pos);

	@Override
	public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_,
		CollisionContext p_220053_4_) {
		return ChuteShapes.getShape(p_220053_1_);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockGetter p_220071_2_, BlockPos p_220071_3_,
		CollisionContext p_220071_4_) {
		return ChuteShapes.getCollisionShape(p_220071_1_);
	}

	@Override
	public Class<ChuteBlockEntity> getBlockEntityClass() {
		return ChuteBlockEntity.class;
	}

	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.isEmpty())
			return InteractionResult.PASS;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		return onBlockEntityUseItemOn(level, pos, be -> {
			if (be.item.isEmpty())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			player.getInventory()
				.placeItemBackInInventory(be.item);
			be.setItem(ItemStack.EMPTY);
			return ItemInteractionResult.SUCCESS;
		}) == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

}
