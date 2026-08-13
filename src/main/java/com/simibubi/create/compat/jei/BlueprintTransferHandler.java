package com.simibubi.create.compat.jei;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket;
import com.simibubi.create.content.equipment.blueprint.BlueprintMenu;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlueprintTransferHandler implements IRecipeTransferHandler<BlueprintMenu, RecipeHolder<CraftingRecipe>> {

	@Override
	public Class<? extends BlueprintMenu> getContainerClass() {
		return BlueprintMenu.class;
	}

	@Override
	public Optional<MenuType<BlueprintMenu>> getMenuType() {
		return Optional.empty();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
		return (RecipeType<RecipeHolder<CraftingRecipe>>) (Object) RecipeTypes.CRAFTING;
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(BlueprintMenu menu, RecipeHolder<CraftingRecipe> craftingRecipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
		if (!doTransfer)
			return null;

		ClientNetworkHelper.INSTANCE.sendToServer(
			new BlueprintAssignCompleteRecipePacket(craftingRecipe.id().identifier()));
		return null;
	}

}
