package net.neoforged.neoforge.client.model.generators;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

public abstract class BlockStateProvider implements DataProvider {
	protected final Map<Block, IGeneratedBlockState> registeredBlocks = new HashMap<>();
	private final String modId;
	private final BlockModelProvider blockModels;

	protected BlockStateProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
		this.modId = modId;
		this.blockModels = new BlockModelProvider(existingFileHelper);
	}

	protected abstract void registerStatesAndModels();

	public BlockModelProvider models() {
		return blockModels;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public String getName() {
		return "Block States: " + modId;
	}
}
