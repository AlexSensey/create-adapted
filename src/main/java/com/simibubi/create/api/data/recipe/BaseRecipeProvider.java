package com.simibubi.create.api.data.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.Create;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;

/**
 * A class containing some basic setup for other recipe generators to use.
 * Addons should extend this if they add a custom recipe type that is not
 * a processing recipe type and want to use Create's helpers.
 * For processing recipes extend {@link StandardProcessingRecipeGen}.
 */
public abstract class BaseRecipeProvider extends RecipeProvider {
	protected final String modid;
	protected final List<GeneratedRecipe> all = new ArrayList<>();

	public BaseRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String defaultNamespace) {
		// TODO 26.2: RecipeProvider is now constructed by the datagen runner with a resolved Provider/RecipeOutput.
		super(null, null);
		this.modid = defaultNamespace;
	}

	/**
	 * 26.2 constructor used by {@link RecipeProvider.Runner}, after registries and
	 * the recipe output have been resolved.
	 */
	protected BaseRecipeProvider(HolderLookup.Provider registries, RecipeOutput output, String defaultNamespace) {
		super(registries, output);
		this.modid = defaultNamespace;
	}

	protected Identifier asResource(String path) {
		return Identifier.fromNamespaceAndPath(modid, path);
	}

	protected GeneratedRecipe register(GeneratedRecipe recipe) {
		all.add(recipe);
		return recipe;
	}

	@Override
	protected void buildRecipes() {
		all.forEach(c -> c.register(output));
		Create.LOGGER.info("{} registered {} recipe{}", getName(), all.size(), all.size() == 1 ? "" : "s");
	}

	public String getName() {
		return modid + "'s recipes";
	}

	@FunctionalInterface
	public interface GeneratedRecipe {
		void register(RecipeOutput recipeOutput);
	}
}
