package com.simibubi.create.content.processing.basin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import com.simibubi.create.foundation.fluid.ResourceHandlerFluidAdapter;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.simibubi.create.foundation.recipe.trie.AbstractVariant;
import com.simibubi.create.foundation.recipe.trie.RecipeTrie;
import com.simibubi.create.foundation.recipe.trie.RecipeTrieFinder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public abstract class BasinOperatingBlockEntity extends KineticBlockEntity {

	public DeferralBehaviour basinChecker;
	public boolean basinRemoved;
	protected Recipe<?> currentRecipe;

	public BasinOperatingBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		basinChecker = new DeferralBehaviour(this, this::updateBasin);
		behaviours.add(basinChecker);
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		if (getSpeed() == 0)
			basinRemoved = true;
		basinRemoved = false;
		basinChecker.scheduleUpdate();
	}

	@Override
	public void tick() {
		if (basinRemoved) {
			basinRemoved = false;
			onBasinRemoved();
			sendData();
			return;
		}

		super.tick();
	}

	protected boolean updateBasin() {
		if (!isSpeedRequirementFulfilled())
			return true;
		if (getSpeed() == 0)
			return true;
		if (isRunning())
			return true;
		if (level == null || level.isClientSide())
			return true;
		Optional<BasinBlockEntity> basin = getBasin();
		if (!basin.filter(BasinBlockEntity::canContinueProcessing)
			.isPresent())
			return true;

		List<Recipe<?>> recipes = getMatchingRecipes();
		if (recipes.isEmpty())
			return true;
		currentRecipe = recipes.get(0);
		startProcessingBasin();
		sendData();
		return true;
	}

	protected abstract boolean isRunning();

	public void startProcessingBasin() {
	}

	public boolean continueWithPreviousRecipe() {
		return true;
	}

	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (recipe == null)
			return false;
		Optional<BasinBlockEntity> basin = getBasin();
		if (!basin.isPresent())
			return false;
		return BasinRecipe.match(basin.get(), recipe);
	}

	protected void applyBasinRecipe() {
		if (currentRecipe == null)
			return;

		Optional<BasinBlockEntity> optionalBasin = getBasin();
		if (!optionalBasin.isPresent())
			return;
		BasinBlockEntity basin = optionalBasin.get();
		boolean wasEmpty = basin.canContinueProcessing();
		if (!BasinRecipe.apply(basin, currentRecipe))
			return;
		getProcessedRecipeTrigger().ifPresent(this::award);
		basin.inputTank.sendDataImmediately();

		// Continue mixing
		if (wasEmpty && matchBasinRecipe(currentRecipe)) {
			continueWithPreviousRecipe();
			sendData();
		}

		basin.notifyChangeOfContents();
	}

	protected List<Recipe<?>> getMatchingRecipes() {
		Optional<BasinBlockEntity> $basin = getBasin();
		BasinBlockEntity basin;
		if ($basin.isEmpty() || (basin = $basin.get()).isEmpty())
			return new ArrayList<>();

		List<Recipe<?>> list = new ArrayList<>();
		try {
			IItemHandler availableItems = basin.itemCapability;
			ResourceHandler<FluidResource> fluidResourceHandler =
				level.getCapability(Capabilities.Fluid.BLOCK, basin.getBlockPos(), null);
			IFluidHandler availableFluids =
				fluidResourceHandler == null ? null : new ResourceHandlerFluidAdapter(fluidResourceHandler);

			if (availableItems == null && availableFluids == null)
				return list;

			RecipeTrie<?> trie = RecipeTrieFinder.get(getRecipeCacheKey(), level, this::matchStaticFilters);
			Set<AbstractVariant> availableVariants = RecipeTrie.getVariants(availableItems, availableFluids);

			for (Recipe<?> recipe : trie.lookup(availableVariants))
				if (matchBasinRecipe(recipe))
					list.add(recipe);
		} catch (Exception e) {
			Create.LOGGER.error("Failed to get recipe trie, falling back to slow logic", e);
			list.clear();

			for (RecipeHolder<? extends Recipe<?>> r : RecipeFinder.get(getRecipeCacheKey(), level, this::matchStaticFilters))
				if (matchBasinRecipe(r.value()))
					list.add(r.value());
		}

		list.sort(Comparator.comparingInt(BasinOperatingBlockEntity::getRecipeIngredientCount)
			.reversed());

		return list;
	}

	private static int getRecipeIngredientCount(Recipe<?> recipe) {
		if (recipe instanceof ProcessingRecipe<?, ?> processingRecipe)
			return processingRecipe.getIngredients()
				.size()
				+ processingRecipe.getFluidIngredients()
					.size();
		return 0;
	}

	protected abstract void onBasinRemoved();

	protected Optional<BasinBlockEntity> getBasin() {
		if (level == null)
			return Optional.empty();
		BlockEntity basinBE = level.getBlockEntity(worldPosition.below(2));
		if (!(basinBE instanceof BasinBlockEntity))
			return Optional.empty();
		return Optional.of((BasinBlockEntity) basinBE);
	}

	protected Optional<CreateAdvancement> getProcessedRecipeTrigger() {
		return Optional.empty();
	}

	protected abstract boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe);

	protected abstract Object getRecipeCacheKey();
}
