package com.simibubi.create.content.redstone.contact;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.TickPriority;

public class ContactMovementBehaviour implements MovementBehaviour {

	@Override
	public Vec3 getActiveAreaOffset(MovementContext context) {
		return Vec3.atLowerCornerOf(context.state.getValue(RedstoneContactBlock.FACING)
			.getUnitVec3i())
			.scale(.65f);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		BlockState block = context.state;
		Level world = context.world;

		if (world.isClientSide())
			return;
		if (context.firstMovement)
			return;

		deactivateLastVisitedContact(context);
		BlockState visitedState = world.getBlockState(pos);
		if (!AllBlocks.REDSTONE_CONTACT.has(visitedState) && !AllBlocks.ELEVATOR_CONTACT.has(visitedState))
			return;

		Vec3 contact = Vec3.atLowerCornerOf(block.getValue(RedstoneContactBlock.FACING)
			.getUnitVec3i());
		contact = context.rotation.apply(contact);
		Direction direction = Direction.getNearest((int) Math.round(contact.x), (int) Math.round(contact.y),
			(int) Math.round(contact.z), null);

		if (visitedState.getValue(RedstoneContactBlock.FACING) != direction.getOpposite())
			return;

		if (AllBlocks.REDSTONE_CONTACT.has(visitedState))
			world.setBlockAndUpdate(pos, visitedState.setValue(RedstoneContactBlock.POWERED, true));
		if (AllBlocks.ELEVATOR_CONTACT.has(visitedState) && context.contraption instanceof ElevatorContraption ec)
			ec.broadcastFloorData(world, pos);

		context.data.put("lastContact", writeBlockPos(pos));
		return;
	}

	@Override
	public void stopMoving(MovementContext context) {
		deactivateLastVisitedContact(context);
	}

	@Override
	public void cancelStall(MovementContext context) {
		MovementBehaviour.super.cancelStall(context);
		deactivateLastVisitedContact(context);
	}

	public void deactivateLastVisitedContact(MovementContext context) {
		if (context.world.isClientSide())
			return;
		if (!context.data.contains("lastContact"))
			return;

		BlockPos last = readBlockPos(context.data, "lastContact");
		context.data.remove("lastContact");
		BlockState blockState = context.world.getBlockState(last);

		if (AllBlocks.REDSTONE_CONTACT.has(blockState))
			context.world.scheduleTick(last, AllBlocks.REDSTONE_CONTACT.get(), 1, TickPriority.NORMAL);
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readBlockPos(CompoundTag tag, String key) {
		Tag value = tag.get(key);
		if (value instanceof CompoundTag compound)
			return new BlockPos(compound.getIntOr("X", 0), compound.getIntOr("Y", 0), compound.getIntOr("Z", 0));
		if (value instanceof ListTag list && list.size() >= 3)
			return new BlockPos(list.getIntOr(0, 0), list.getIntOr(1, 0), list.getIntOr(2, 0));
		return BlockPos.of(tag.getLongOr(key, 0));
	}

}
