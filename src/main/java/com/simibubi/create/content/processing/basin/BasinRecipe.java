package com.simibubi.create.content.processing.basin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

public class BasinRecipe extends StandardProcessingRecipe<RecipeInput> {
	// Runtime-only origin of a converted crafting recipe; never serialized.
	private CraftingRecipe craftingSource;

	public static boolean match(BasinBlockEntity basin, Recipe<?> recipe) {
		FilteringBehaviour filter = basin.getFilter();
		if (filter == null)
			return false;

		boolean filterTest = recipe instanceof BasinRecipe basinRecipe
			&& !basinRecipe.getRollableResults().isEmpty()
			&& filter.test(basinRecipe.getRollableResults().get(0).getStack());
		if (recipe instanceof BasinRecipe basinRecipe) {
			if (basinRecipe.getRollableResults()
				.isEmpty()
				&& !basinRecipe.getFluidResults()
				.isEmpty())
				filterTest = filter.test(basinRecipe.getFluidResults()
					.get(0));
		}

		if (!filterTest)
			return false;

		return apply(basin, recipe, true);
	}

	public static boolean apply(BasinBlockEntity basin, Recipe<?> recipe) {
		return apply(basin, recipe, false);
	}

	private static boolean apply(BasinBlockEntity basin, Recipe<?> recipe, boolean test) {
		if (!(recipe instanceof BasinRecipe))
			return false;
		boolean isBasinRecipe = recipe instanceof BasinRecipe;
		IItemHandler availableItems = basin.itemCapability;
		IFluidHandler availableFluids = basin.fluidCapability;

		if (availableItems == null || availableFluids == null)
			return false;

		HeatLevel heat = basin.getHeatLevel();
		if (isBasinRecipe && !((BasinRecipe) recipe).getRequiredHeat()
			.testBlazeBurner(heat))
			return false;

		List<ItemStack> recipeOutputItems = new ArrayList<>();
		List<FluidStack> recipeOutputFluids = new ArrayList<>();

		List<Ingredient> ingredients = new LinkedList<>(((BasinRecipe) recipe).getIngredients());
		List<SizedFluidIngredient> fluidIngredients =
			isBasinRecipe ? ((BasinRecipe) recipe).getFluidIngredients() : Collections.emptyList();

		for (boolean simulate : Iterate.trueAndFalse) {

			if (!simulate && test)
				return true;

			int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
			List<ItemStack> matchedItems = new ArrayList<>();
			int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];

			Ingredients:
			for (Ingredient ingredient : ingredients) {
				for (int slot = 0; slot < availableItems.getSlots(); slot++) {
					if (simulate && availableItems.getStackInSlot(slot)
						.getCount() <= extractedItemsFromSlot[slot])
						continue;
					ItemStack extracted = availableItems.extractItem(slot, 1, true);
					if (!ingredient.test(extracted))
						continue;
					if (simulate)
						matchedItems.add(extracted.copyWithCount(1));
					if (!simulate)
						availableItems.extractItem(slot, 1, false);
					extractedItemsFromSlot[slot]++;
					continue Ingredients;
				}

				// something wasn't found
				return false;
			}

			boolean fluidsAffected = false;
			FluidIngredients:
			for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
				int amountRequired = fluidIngredient.amount();

				for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
					FluidStack fluidStack = availableFluids.getFluidInTank(tank);
					int availableAmount = fluidStack.getAmount() - extractedFluidsFromTank[tank];
					if (availableAmount <= 0)
						continue;
					FluidStack availableStack = FluidHelper.copyStackWithAmount(fluidStack, availableAmount);
					if (!fluidIngredient.test(availableStack))
						continue;
					int drainedAmount = Math.min(amountRequired, availableAmount);
					if (!simulate) {
						FluidStack drained = availableFluids.drain(FluidHelper.copyStackWithAmount(fluidStack, drainedAmount),
							FluidAction.EXECUTE);
						if (drained.getAmount() != drainedAmount)
							return false;
						fluidsAffected = true;
					}
					amountRequired -= drainedAmount;
					extractedFluidsFromTank[tank] += drainedAmount;
					if (amountRequired != 0)
						continue;
					continue FluidIngredients;
				}

				// something wasn't found
				return false;
			}

			if (fluidsAffected) {
				basin.getBehaviour(SmartFluidTankBehaviour.INPUT)
					.forEach(TankSegment::onFluidStackChanged);
				basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT)
					.forEach(TankSegment::onFluidStackChanged);
			}

			if (simulate) {
				BasinRecipe basinRecipe = (BasinRecipe) recipe;
				if (basinRecipe.craftingSource != null) {
					CraftingRecipe source = basinRecipe.craftingSource;
					CraftingInput input = createCraftingInput(source, matchedItems);
					if (!source.matches(input, basin.getLevel()))
						return false;
					ItemStack result = source.assemble(input);
					if (result.isEmpty() || !basin.getFilter().test(result))
						return false;
					recipeOutputItems.add(result.copy());
					// Include every returned container in the same capacity check as
					// the product, before consuming anything from the basin.
					for (ItemStack remainder : source.getRemainingItems(input))
						if (!remainder.isEmpty())
							recipeOutputItems.add(remainder.copy());
				} else {
					recipeOutputItems.addAll(basinRecipe.rollResults(basin.getLevel().getRandom()));

					for (FluidStack fluidStack : basinRecipe.getFluidResults())
						if (!fluidStack.isEmpty())
							recipeOutputFluids.add(fluidStack);
				}
			}

			if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate))
				return false;
		}

		return true;
	}

	public static RecipeHolder<BasinRecipe> convertShapeless(RecipeHolder<?> recipe, Level level) {
		Recipe<?> source = recipe.value();
		List<Ingredient> ingredients = source instanceof ShapedRecipe shaped
			? shaped.getIngredients().stream().flatMap(java.util.Optional::stream).toList()
			: source.placementInfo().ingredients();
		ItemStack result = source.display().stream().findFirst()
			.map(display -> display.result().resolveForFirstStack(
				SlotDisplayContext.fromLevel(level)))
			.orElse(ItemStack.EMPTY);
		Builder<BasinRecipe> builder = new Builder<>(BasinRecipe::new, recipe.id().identifier())
			.withItemIngredients(ingredients.toArray(Ingredient[]::new));
		if (!result.isEmpty())
			builder.withSingleItemOutput(result);
		BasinRecipe basinRecipe = builder.build();
		if (source instanceof CraftingRecipe crafting)
			basinRecipe.craftingSource = crafting;
		return new RecipeHolder<>(recipe.id(), basinRecipe);
	}

	private static CraftingInput createCraftingInput(CraftingRecipe source, List<ItemStack> matchedItems) {
		if (source instanceof ShapedRecipe shaped) {
			List<ItemStack> slots = new ArrayList<>();
			int ingredient = 0;
			for (var slot : shaped.getIngredients())
				slots.add(slot.isPresent() ? matchedItems.get(ingredient++).copy() : ItemStack.EMPTY);
			return CraftingInput.of(shaped.getWidth(), shaped.getHeight(), slots);
		}
		return CraftingInput.of(matchedItems.size(), 1, matchedItems);
	}

	protected BasinRecipe(IRecipeTypeInfo type, ProcessingRecipeParams params) {
		super(type, params);
	}

	public BasinRecipe(ProcessingRecipeParams params) {
		this(AllRecipeTypes.BASIN, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 64;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 2;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 2;
	}

	@Override
	protected boolean canRequireHeat() {
		return true;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	public boolean matches(RecipeInput input, @NotNull Level worldIn) {
		return false;
	}

}
