package com.simibubi.create.content.kinetics.deployer;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;


public class DeployerApplicationRecipe extends ItemApplicationRecipe implements IAssemblyRecipe {

	public DeployerApplicationRecipe(ItemApplicationRecipeParams params) {
		super(AllRecipeTypes.DEPLOYING, params);
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	public static RecipeHolder<DeployerApplicationRecipe> convert(RecipeHolder<?> sandpaperRecipe) {
		Identifier id = sandpaperRecipe.id().identifier().withSuffix("_using_deployer");
		ItemApplicationRecipe.Builder<DeployerApplicationRecipe> builder = new ItemApplicationRecipe.Builder<>(DeployerApplicationRecipe::new, id);

		if (sandpaperRecipe.value() instanceof com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?> recipe) {
			if (!recipe.getIngredients().isEmpty())
				builder.require(recipe.getIngredients().getFirst());
			builder.require(AllItems.SAND_PAPER.get());
			for (ProcessingOutput output : recipe.getRollableResults())
				builder.output(output);
			builder.toolNotConsumed();
		}

		return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), builder.build());
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {
		list.add(ingredients.get(1));
	}

	@Override
	public Component getDescriptionForAssembly() {
		return CreateLang.translateDirect("recipe.assembly.deploying_item", "");
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(AllBlocks.DEPLOYER.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> SequencedAssemblySubCategory.AssemblyDeploying::new;
	}

}
