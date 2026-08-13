package com.simibubi.create.foundation.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/** Incremental bridge from Create's recipe generators to the 26.2 runner API. */
public final class RecipeData26 extends RecipeProvider {
	private RecipeData26(HolderLookup.Provider registries, RecipeOutput output) {
		super(registries, output);
	}

	@Override
	protected void buildRecipes() {
	}

	public static void gatherData(GatherDataEvent.Server event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();
		add(generator, output, registries, "compacting", CreateCompactingRecipeGen::new);
		add(generator, output, registries, "crushing", CreateCrushingRecipeGen::new);
		add(generator, output, registries, "cutting", CreateCuttingRecipeGen::new);
		add(generator, output, registries, "deploying", CreateDeployingRecipeGen::new);
		add(generator, output, registries, "emptying", CreateEmptyingRecipeGen::new);
		add(generator, output, registries, "filling", CreateFillingRecipeGen::new);
		add(generator, output, registries, "haunting", CreateHauntingRecipeGen::new);
		add(generator, output, registries, "item application", CreateItemApplicationRecipeGen::new);
		add(generator, output, registries, "milling", CreateMillingRecipeGen::new);
		add(generator, output, registries, "mixing", CreateMixingRecipeGen::new);
		add(generator, output, registries, "mechanical crafting", CreateMechanicalCraftingRecipeGen::new);
		add(generator, output, registries, "polishing", CreatePolishingRecipeGen::new);
		add(generator, output, registries, "pressing", CreatePressingRecipeGen::new);
		add(generator, output, registries, "sequenced assembly", CreateSequencedAssemblyRecipeGen::new);
		add(generator, output, registries, "standard", CreateStandardRecipeGen::new);
		add(generator, output, registries, "washing", CreateWashingRecipeGen::new);
	}

	private static void add(DataGenerator generator, PackOutput output,
		CompletableFuture<HolderLookup.Provider> registries, String name, GeneratorFactory factory) {
		generator.addProvider(true, new GeneratorRunner(output, registries, name, factory));
	}

	@FunctionalInterface
	private interface GeneratorFactory {
		RecipeProvider create(HolderLookup.Provider registries, RecipeOutput output);
	}

	private static final class GeneratorRunner extends Runner {
		private final String name;
		private final GeneratorFactory factory;

		private GeneratorRunner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
			String name, GeneratorFactory factory) {
			super(output, registries);
			this.name = name;
			this.factory = factory;
		}

		@Override
		protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
			// Create's late-registered item holders have not received their default
			// components yet when server datagen runs. Recipes only need a valid stack
			// prototype; the actual defaults are not emitted into recipe JSON.
			BuiltInRegistries.ITEM.listElements()
				.filter(holder -> !holder.areComponentsBound())
				.forEach(holder -> holder.bindComponents(DataComponentMap.EMPTY));
			BuiltInRegistries.FLUID.listElements()
				.filter(holder -> !holder.areComponentsBound())
				.forEach(holder -> holder.bindComponents(DataComponentMap.EMPTY));
			return factory.create(registries, output);
		}

		@Override
		public String getName() {
			return "Create " + name + " recipes (26.2)";
		}
	}
}
