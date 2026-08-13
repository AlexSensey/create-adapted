package com.simibubi.create.foundation.recipe;

import java.util.Collection;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

public final class CreateRecipeClientCache {
	private static RecipeMap recipes = RecipeMap.EMPTY;

	private CreateRecipeClientCache() {}

	public static void onRecipesReceived(RecipesReceivedEvent event) {
		if (!event.getRecipeTypes().contains(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType()))
			return;
		recipes = event.getRecipeMap();
	}

	public static Collection<RecipeHolder<?>> getRecipes() {
		return recipes.values();
	}

	public static RecipeHolder<?> getRecipe(Identifier id) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
		return recipes.byKey(key);
	}

	@SuppressWarnings("unchecked")
	public static <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(RecipeType<R> type,
		I input, Level level) {
		for (RecipeHolder<?> holder : recipes.values()) {
			Recipe<?> recipe = holder.value();
			if (recipe.getType() != type)
				continue;
			R typedRecipe = (R) recipe;
			if (typedRecipe.matches(input, level))
				return Optional.of((RecipeHolder<R>) holder);
		}
		return Optional.empty();
	}

	public static void clear() {
		recipes = RecipeMap.EMPTY;
	}
}
