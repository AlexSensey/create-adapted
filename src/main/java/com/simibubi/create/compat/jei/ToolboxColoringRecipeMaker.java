package com.simibubi.create.compat.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class ToolboxColoringRecipeMaker {

	// From JEI's ShulkerBoxColoringRecipeMaker
	public static Stream<RecipeHolder<CraftingRecipe>> createRecipes() {
		String group = "create.toolbox.color";
		Ingredient baseShulkerIngredient = Ingredient.of(AllBlocks.TOOLBOXES.get(DyeColor.BROWN).get());

		return Arrays.stream(DyeColor.values())
			.filter(dc -> dc != DyeColor.BROWN)
			.map(color -> {
				var dye = BuiltInRegistries.ITEM.getValue(
					Identifier.withDefaultNamespace(color.getName() + "_dye"));
				Ingredient colorIngredient = Ingredient.of(dye);
				Block coloredShulkerBox = AllBlocks.TOOLBOXES.get(color)
					.get();
				ItemStack output = new ItemStack(coloredShulkerBox);
				ShapelessRecipe recipe = new ShapelessRecipe(new Recipe.CommonInfo(false),
					new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, group),
					ItemStackTemplate.fromStack(output), List.of(baseShulkerIngredient, colorIngredient));
				return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
					Create.asResource(group + "/" + color)), recipe);
			});
	}

	private ToolboxColoringRecipeMaker() {}

}
