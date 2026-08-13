package com.simibubi.create.content.logistics.stockTicker;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.BigItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

public class CraftableBigItemStack extends BigItemStack {

	public Recipe<?> recipe;

	public CraftableBigItemStack(ItemStack stack, Recipe<?> recipe) {
		super(stack);
		count = 0;
		this.recipe = recipe;
	}

	public List<Ingredient> getIngredients() {
		return ingredientsOf(recipe);
	}

	public static List<Ingredient> ingredientsOf(Recipe<?> recipe) {
		PlacementInfo placement = recipe.placementInfo();
		List<Ingredient> result = new ArrayList<>();
		for (int ingredientIndex : placement.slotsToIngredientIndex()) {
			if (ingredientIndex == PlacementInfo.EMPTY_SLOT)
				continue;
			if (ingredientIndex >= 0 && ingredientIndex < placement.ingredients().size())
				result.add(placement.ingredients().get(ingredientIndex));
		}
		return result;
	}

	public int getOutputCount(Level level) {
		return stack.getCount();
	}

}
