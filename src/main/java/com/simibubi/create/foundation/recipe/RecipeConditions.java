package com.simibubi.create.foundation.recipe;

import java.util.function.Predicate;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Commonly used Predicates for searching through recipe collections.
 *
 * @author simibubi
 *
 */
public class RecipeConditions {

	public static Predicate<RecipeHolder<? extends Recipe<?>>> isOfType(RecipeType<?>... otherTypes) {
		return recipe -> {
			RecipeType<?> recipeType = recipe.value().getType();
			for (RecipeType<?> other : otherTypes)
				if (recipeType == other)
					return true;
			return false;
		};
	}

	public static Predicate<RecipeHolder<? extends Recipe<?>>> firstIngredientMatches(ItemStack stack) {
		return r -> {
			Ingredient ingredient = firstIngredientOf(r.value());
			return ingredient != null && ingredient.test(stack);
		};
	}

	public static Predicate<RecipeHolder<? extends Recipe<?>>> outputMatchesFilter(FilteringBehaviour filtering) {
		return r -> !filtering.isActive() || filtering.test(resultOf(r.value()));

	}

	private static Ingredient firstIngredientOf(Recipe<?> recipe) {
		if (recipe instanceof ProcessingRecipe<?, ?> processingRecipe)
			return processingRecipe.getIngredients()
				.isEmpty() ? null : processingRecipe.getIngredients()
				.get(0);
		if (recipe instanceof StonecutterRecipe stonecutterRecipe)
			return stonecutterRecipe.input();
		return null;
	}

	private static ItemStack resultOf(Recipe<?> recipe) {
		if (recipe instanceof ProcessingRecipe<?, ?> processingRecipe)
			return processingRecipe.getResultItem(null);
		if (recipe instanceof StonecutterRecipe stonecutterRecipe) {
			Ingredient input = stonecutterRecipe.input();
			ItemStack stack = input.items()
				.map(holder -> new ItemStack(holder.value()))
				.findFirst()
				.orElse(ItemStack.EMPTY);
			return stack.isEmpty() ? ItemStack.EMPTY : stonecutterRecipe.assemble(new SingleRecipeInput(stack));
		}
		return ItemStack.EMPTY;
	}

}
