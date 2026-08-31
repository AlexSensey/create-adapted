package net.neoforged.neoforge.client.model.generators;

import java.util.concurrent.CompletableFuture;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

public abstract class ItemModelProvider implements DataProvider {
	private final String modId;

	protected ItemModelProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
		this.modId = modId;
	}

	protected abstract void registerModels();

	public ResourceLocation modLoc(String path) {
		return ResourceLocation.fromNamespaceAndPath(modId, path);
	}

	public ModelBuilder<?> withExistingParent(String name, ResourceLocation parent) {
		return new ItemModelBuilder();
	}

	public ModelBuilder<?> withExistingParent(String name, String parent) {
		return new ItemModelBuilder();
	}

	public ModelBuilder<?> getBuilder(String name) {
		return new ItemModelBuilder();
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public String getName() {
		return "Item Models: " + modId;
	}
}
