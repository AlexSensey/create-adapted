package com.simibubi.create.content.fluids.transfer;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.createmod.catnip.api.data.Pair;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class GenericItemEmptying {

	public static boolean canItemBeEmptied(Level world, ItemStack stack) {
		if (PotionFluidHandler.isPotionItem(stack))
			return true;
		if (stack.is(Items.MILK_BUCKET))
			return true;

		if (AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), world)
			.isPresent())
			return true;

		if (stack.isEmpty())
			return false;
		ResourceHandler<FluidResource> capability = ItemAccess.forStack(stack.copy())
			.oneByOne()
			.getCapability(Capabilities.Fluid.ITEM);
		if (capability == null)
			return false;
		for (int i = 0; i < capability.size(); i++) {
			if (!capability.getResource(i)
				.isEmpty()
				&& capability.getAmountAsLong(i) > 0)
				return true;
		}
		return false;
	}

	public static Pair<FluidStack, ItemStack> emptyItem(Level level, ItemStack stack, boolean simulate) {
		FluidStack resultingFluid = FluidStack.EMPTY;
		ItemStack resultingItem = ItemStack.EMPTY;

		if (stack.isEmpty())
			return Pair.of(resultingFluid, resultingItem);

		if (PotionFluidHandler.isPotionItem(stack))
			return PotionFluidHandler.emptyPotion(stack, simulate);

		// Milk buckets are not BucketItems and their transfer capability is not
		// guaranteed to be present in 26.2. Treat them explicitly so they also work
		// as Smart Fluid Pipe filters.
		if (stack.is(Items.MILK_BUCKET)) {
			if (!simulate)
				stack.shrink(1);
			return Pair.of(new FluidStack(NeoForgeMod.MILK.get(), 1000), new ItemStack(Items.BUCKET));
		}

		if (stack.getItem() instanceof BucketItem bucketItem && stack.getItem() != Items.BUCKET) {
			Fluid fluid = FluidHelper.convertToStill(bucketItem.getContent());
			if (!fluid.isSame(Fluids.EMPTY)) {
				if (!simulate)
					stack.shrink(1);
				return Pair.of(new FluidStack(fluid, 1000), new ItemStack(Items.BUCKET));
			}
		}

		Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe = AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), level);
		if (recipe.isPresent()) {
			EmptyingRecipe emptyingRecipe = (EmptyingRecipe) recipe.get().value();
			List<ItemStack> results = emptyingRecipe.rollResults(level.getRandom());
			if (!simulate)
				stack.shrink(1);
			resultingItem = results.isEmpty() ? ItemStack.EMPTY : results.get(0);
			resultingFluid = emptyingRecipe.getResultingFluid();
			return Pair.of(resultingFluid, resultingItem);
		}

		ItemStack split = stack.copy();
		split.setCount(1);
		ItemAccess itemAccess = ItemAccess.forStack(split)
			.oneByOne();
		ResourceHandler<FluidResource> capability = itemAccess.getCapability(Capabilities.Fluid.ITEM);
		if (capability == null)
			return Pair.of(resultingFluid, resultingItem);
		try (Transaction transaction = Transaction.openRoot()) {
			var extracted = ResourceHandlerUtil.extractFirst(capability, $ -> true, 1000, transaction);
			if (extracted == null)
				return Pair.of(resultingFluid, resultingItem);
			resultingFluid = extracted.resource()
				.toStack(extracted.amount());
			if (!simulate) {
				transaction.commit();
				resultingItem = itemAccess.getResource()
					.toStack(itemAccess.getAmount());
				stack.shrink(1);
			} else {
				resultingItem = itemAccess.getResource()
					.toStack(itemAccess.getAmount());
			}
		}

		return Pair.of(resultingFluid, resultingItem);
	}

}
