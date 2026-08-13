package com.simibubi.create.content.trains.signal;

import com.simibubi.create.content.trains.graph.DimensionPalette;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class SingleBlockEntityEdgePoint extends TrackEdgePoint {

	public ResourceKey<Level> blockEntityDimension;
	public BlockPos blockEntityPos;

	public BlockPos getBlockEntityPos() {
		return blockEntityPos;
	}

	public ResourceKey<Level> getBlockEntityDimension() {
		return blockEntityDimension;
	}

	@Override
	public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
		this.blockEntityPos = blockEntity.getBlockPos();
		this.blockEntityDimension = blockEntity.getLevel()
			.dimension();
	}

	@Override
	public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
		removeFromAllGraphs();
	}

	@Override
	public void invalidate(LevelAccessor level) {
		invalidateAt(level, blockEntityPos);
	}

	@Override
	public boolean canMerge() {
		return false;
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean migration, DimensionPalette dimensions) {
		super.read(nbt, registries, migration, dimensions);
		if (migration)
			return;
		blockEntityPos = readBlockPos(nbt.getCompoundOrEmpty("BlockEntityPos"));
		blockEntityDimension = dimensions.decode(nbt.contains("BlockEntityDimension") ? nbt.getIntOr("BlockEntityDimension", -1) : -1);
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
		super.write(nbt, registries, dimensions);
		nbt.put("BlockEntityPos", writeBlockPos(blockEntityPos));
		nbt.putInt("BlockEntityDimension", dimensions.encode(blockEntityDimension));
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readBlockPos(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
	}

}
