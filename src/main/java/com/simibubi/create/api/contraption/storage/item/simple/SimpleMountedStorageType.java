package com.simibubi.create.api.contraption.storage.item.simple;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public abstract class SimpleMountedStorageType<T extends SimpleMountedStorage> extends MountedItemStorageType<SimpleMountedStorage> {
	protected SimpleMountedStorageType(MapCodec<T> codec) {
		super(codec);
	}

	@Override
	@Nullable
	public SimpleMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		return Optional.ofNullable(be)
			.map(b -> getHandler(level, b))
			.map(this::createStorage)
			.orElse(null);
	}

	protected IItemHandler getHandler(Level level, BlockEntity be) {
		ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, be.getBlockPos(), null);
		return handler == null ? null : new ResourceHandlerItemAdapter(handler);
	}

	protected SimpleMountedStorage createStorage(IItemHandler handler) {
		return new SimpleMountedStorage(this, handler);
	}

	public static final class Impl extends SimpleMountedStorageType<SimpleMountedStorage> {
		public Impl() {
			super(SimpleMountedStorage.CODEC);
		}
	}
}
