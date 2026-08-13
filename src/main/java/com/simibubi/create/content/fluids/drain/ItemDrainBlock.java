package com.simibubi.create.content.fluids.drain;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ItemDrainBlock extends Block implements IWrenchable, IBE<ItemDrainBlockEntity> {

	public ItemDrainBlock(Properties p_i48440_1_) {
		super(p_i48440_1_);
	}

	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (stack.getItem() instanceof BlockItem && ItemAccess.forStack(stack.copy())
			.oneByOne()
			.getCapability(Capabilities.Fluid.ITEM) == null)
			return InteractionResult.PASS;

		ItemInteractionResult result = onBlockEntityUseItemOn(level, pos, be -> {
			if (!stack.isEmpty()) {
				be.internalTank.allowInsertion();
				ItemInteractionResult tryExchange = tryExchange(level, player, hand, stack, be);
				be.internalTank.forbidInsertion();
				if (tryExchange == ItemInteractionResult.SUCCESS)
					return tryExchange;
			}

			ItemStack heldItemStack = be.getHeldItemStack();
			if (!level.isClientSide() && !heldItemStack.isEmpty()) {
				player.getInventory()
					.placeItemBackInInventory(heldItemStack);
				be.heldItem = null;
				be.notifyUpdate();
			}
			return ItemInteractionResult.SUCCESS;
		});
		return result == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
		updateEntityAfterFallOn(worldIn, entityIn.blockPosition(), entityIn);
	}

	private void updateEntityAfterFallOn(BlockGetter worldIn, BlockPos drainPos, Entity entityIn) {
		if (!(entityIn instanceof ItemEntity itemEntity))
			return;
		if (!entityIn.isAlive())
			return;
		if (entityIn.level().isClientSide())
			return;

		DirectBeltInputBehaviour inputBehaviour =
			BlockEntityBehaviour.get(worldIn, entityIn.blockPosition(), DirectBeltInputBehaviour.TYPE);
		if (inputBehaviour == null)
			return;
		Vec3 deltaMovement = entityIn.getDeltaMovement()
			.multiply(1, 0, 1)
			.normalize();
		if (deltaMovement.lengthSqr() < 1.0e-6) {
			deltaMovement = new Vec3(entityIn.getX() - entityIn.xo, 0, entityIn.getZ() - entityIn.zo)
				.normalize();
		}
		if (deltaMovement.lengthSqr() < 1.0e-6 && itemEntity.getOwner() != null) {
			deltaMovement = Vec3.atCenterOf(drainPos)
				.subtract(itemEntity.getOwner()
					.position())
				.multiply(1, 0, 1)
				.normalize();
		}
		if (deltaMovement.lengthSqr() < 1.0e-6) {
			deltaMovement = Vec3.atCenterOf(drainPos)
				.subtract(entityIn.position())
				.multiply(1, 0, 1)
				.normalize();
		}
		Direction nearest;
		if (Math.abs(deltaMovement.x) > Math.abs(deltaMovement.z))
			nearest = deltaMovement.x > 0 ? Direction.EAST : Direction.WEST;
		else if (Math.abs(deltaMovement.z) > 1.0e-6)
			nearest = deltaMovement.z > 0 ? Direction.SOUTH : Direction.NORTH;
		else
			nearest = Direction.NORTH;
		ItemStack remainder = inputBehaviour.handleInsertion(itemEntity.getItem(), nearest, false);
		itemEntity.setItem(remainder);
		if (remainder.isEmpty())
			itemEntity.discard();
	}

	protected ItemInteractionResult tryExchange(Level worldIn, Player player, InteractionHand handIn, ItemStack heldItem,
											ItemDrainBlockEntity be) {
		if (FluidHelper.tryEmptyItemIntoBE(worldIn, player, handIn, heldItem, be))
			return ItemInteractionResult.SUCCESS;
		if (GenericItemEmptying.canItemBeEmptied(worldIn, heldItem))
			return ItemInteractionResult.SUCCESS;
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		updateEntityAfterFallOn(level, pos, entity);
		super.fallOn(level, state, pos, entity, fallDistance);
	}

	@Override
	public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_,
							   CollisionContext p_220053_4_) {
		return AllShapes.CASING_13PX.get(Direction.UP);
	}

	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			ItemStack heldItemStack = be.getHeldItemStack();
			if (!heldItemStack.isEmpty())
				Containers.dropItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), heldItemStack);
		});
		worldIn.removeBlockEntity(pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
		onRemove(state, level, pos, level.getBlockState(pos), isMoving);
		super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
	}

	@Override
	public Class<ItemDrainBlockEntity> getBlockEntityClass() {
		return ItemDrainBlockEntity.class;
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public BlockEntityType<? extends ItemDrainBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ITEM_DRAIN.get();
	}

	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos, Direction direction) {
		return ComparatorUtil.levelOfSmartFluidTank(worldIn, pos);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

}
