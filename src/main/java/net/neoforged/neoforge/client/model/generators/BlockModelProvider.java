package net.neoforged.neoforge.client.model.generators;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class BlockModelProvider {
	public final ExistingFileHelper existingFileHelper;

	public BlockModelProvider(ExistingFileHelper existingFileHelper) {
		this.existingFileHelper = existingFileHelper;
	}

	public ModelFile.ExistingModelFile getExistingFile(Identifier location) {
		return new ModelFile.ExistingModelFile(location, existingFileHelper);
	}

	public ModelBuilder<?> withExistingParent(String name, Identifier parent) {
		return new BlockModelBuilder(name).parent(getExistingFile(parent));
	}

	public ModelBuilder<?> withExistingParent(String name, ResourceLocation parent) {
		return new BlockModelBuilder(name).parent(new ModelFile.ExistingModelFile(parent, existingFileHelper));
	}
}
