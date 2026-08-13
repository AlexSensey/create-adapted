package com.simibubi.create.content.logistics.vault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class ItemVaultCTBehaviour extends ConnectedTextureBehaviour.Base {

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
		boolean small = !ItemVaultBlock.isLarge(state);
		if (vaultBlockAxis == null)
			return null;

		if (direction.getAxis() == vaultBlockAxis)
			return AllSpriteShifts.VAULT_FRONT.get(small);
		if (direction == Direction.UP)
			return AllSpriteShifts.VAULT_TOP.get(small);
		if (direction == Direction.DOWN)
			return AllSpriteShifts.VAULT_BOTTOM.get(small);

		return AllSpriteShifts.VAULT_SIDE.get(small);
	}

	@Override
	protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
		boolean alongX = vaultBlockAxis == Axis.X;
		if (face.getAxis()
			.isVertical() && alongX)
			return super.getUpDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == vaultBlockAxis || face.getAxis()
			.isVertical())
			return super.getUpDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(vaultBlockAxis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
	}

	@Override
	protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
		if (face.getAxis()
			.isVertical() && vaultBlockAxis == Axis.X)
			return super.getRightDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == vaultBlockAxis || face.getAxis()
			.isVertical())
			return super.getRightDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
	}

	public boolean buildContextForOccludedDirections() {
		return super.buildContextForOccludedDirections();
	}

	@Override
	public CTType getDataType(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
		if (direction.getAxis() == ItemVaultBlock.getVaultBlockAxis(state))
			if (!hasCompleteFaceSection(world, pos, state))
				return null;
		return super.getDataType(world, pos, state, direction);
	}

	private boolean hasCompleteFaceSection(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		ItemVaultBlockEntity vault = ConnectivityHandler.partAt(AllBlockEntityTypes.ITEM_VAULT.get(), world, pos);
		if (vault == null)
			return false;
		ItemVaultBlockEntity controller = vault.getControllerBE();
		if (controller == null)
			return false;

		int width = controller.getWidth();
		if (width <= 1)
			return false;

		Axis axis = ItemVaultBlock.getVaultBlockAxis(state);
		BlockPos origin = controller.getBlockPos();
		int slice = switch (axis) {
			case X -> pos.getX() - origin.getX();
			case Z -> pos.getZ() - origin.getZ();
			default -> 0;
		};

		for (int first = 0; first < width; first++) {
			for (int second = 0; second < width; second++) {
				BlockPos sectionPos = axis == Axis.X
					? origin.offset(slice, first, second)
					: origin.offset(first, second, slice);
				ItemVaultBlockEntity part =
					ConnectivityHandler.partAt(AllBlockEntityTypes.ITEM_VAULT.get(), world, sectionPos);
				if (part == null || !controller.getBlockPos()
					.equals(part.getController()))
					return false;
			}
		}
		return true;
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return state == other && ConnectivityHandler.isConnected(reader, pos, otherPos); //ItemVaultConnectivityHandler.isConnected(reader, pos, otherPos);
	}

}
