package net.neoforged.neoforge.client.model.generators;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.Identifier;

public class ModelBuilder<T extends ModelBuilder<T>> extends ModelFile {
	public ModelBuilder() {
	}

	public ModelBuilder(String location) {
	}

	@SuppressWarnings("unchecked")
	public T parent(ModelFile parent) {
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T texture(String key, ResourceLocation texture) {
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T texture(String key, Identifier texture) {
		return (T) this;
	}
}
