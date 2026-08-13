package com.simibubi.create.foundation.blockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.serialization.MapCodec;

import com.simibubi.create.foundation.utility.GlobalRegistryAccess;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class SyncedBlockEntity extends BlockEntity {
	private static final String CREATE_DATA_KEY = "CreateData";
	private static final MapCodec<CompoundTag> COMPOUND_MAP_CODEC = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

	public SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return writeClient(new CompoundTag(), registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		CompoundTag tag = input.read(COMPOUND_MAP_CODEC)
			.or(() -> input.read(CREATE_DATA_KEY, CompoundTag.CODEC))
			.orElseGet(CompoundTag::new);
		tag.remove(CREATE_DATA_KEY);
		if (level != null && level.isClientSide())
			readClient(tag, input.lookup());
		else
			loadAdditional(tag, input.lookup());
	}

	@Override
	public void onDataPacket(Connection connection, ValueInput input) {
		CompoundTag tag = input.read(COMPOUND_MAP_CODEC)
			.or(() -> input.read(CREATE_DATA_KEY, CompoundTag.CODEC))
			.orElseGet(CompoundTag::new);
		tag.remove(CREATE_DATA_KEY);
		readClient(tag, input.lookup());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		HolderLookup.Provider registries = level != null ? level.registryAccess() : GlobalRegistryAccess.get();
		if (registries == null)
			return;

		CompoundTag tag = new CompoundTag();
		saveAdditional(tag, registries);
		if (!tag.isEmpty())
			output.store(tag);
	}

	// Special handling for client update packets
	public void readClient(CompoundTag tag, HolderLookup.Provider registries) {
		loadAdditional(tag, registries);
	}

	// Special handling for client update packets
	public CompoundTag writeClient(CompoundTag tag, HolderLookup.Provider registries) {
		saveAdditional(tag, registries);
		return tag;
	}

	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	}

	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	}

	public void sendData() {
		if (level instanceof ServerLevel serverLevel) {
			setChanged();
			serverLevel.getChunkSource()
				.blockChanged(getBlockPos());
		}
	}

	public void notifyUpdate() {
		setChanged();
		sendData();
	}

	public HolderGetter<Block> blockHolderGetter() {
		return BuiltInRegistries.BLOCK;
	}
}
