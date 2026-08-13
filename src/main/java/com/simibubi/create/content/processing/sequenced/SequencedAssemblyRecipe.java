package com.simibubi.create.content.processing.sequenced;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class SequencedAssemblyRecipe implements Recipe<RecipeWrapper> {
	protected SequencedAssemblyRecipeSerializer serializer;

	protected Ingredient ingredient;
	protected List<SequencedRecipe<?>> sequence;
	protected int loops;
	protected ProcessingOutput transitionalItem;

	public final List<ProcessingOutput> resultPool;
	private static final RecipeBookCategory SEQUENCED_ASSEMBLY_CATEGORY = new RecipeBookCategory();

	public SequencedAssemblyRecipe(SequencedAssemblyRecipeSerializer serializer) {
		this.serializer = serializer;
		sequence = new ArrayList<>();
		resultPool = new ArrayList<>();
		loops = 5;
	}

	public static <I extends RecipeInput, R extends ProcessingRecipe<I, ?>> Optional<RecipeHolder<R>> getRecipe(Level world, I inv,
																												RecipeType<R> type, Class<R> recipeClass) {
		return getRecipe(world, inv, type, recipeClass, r -> r.value().matches(inv, world));
	}

	public static <I extends RecipeInput, R extends ProcessingRecipe<I, ?>> Optional<RecipeHolder<R>> getRecipe(Level world, I inv,
																												RecipeType<R> type, Class<R> recipeClass, Predicate<? super RecipeHolder<R>> recipeFilter) {
		List<RecipeHolder<R>> list = getRecipes(world, inv.getItem(0), type, recipeClass, recipeFilter);

		if (!list.isEmpty()) {
			return Optional.of(list.getFirst());
		} else {
			return Optional.empty();
		}
	}

	public static <R extends ProcessingRecipe<?, ?>> Optional<RecipeHolder<R>> getRecipe(Level level, ItemStack item,
																						 RecipeType<R> type, Class<R> recipeClass) {
		List<RecipeHolder<R>> list = getRecipes(level, item, type, recipeClass, r -> true);
		return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
	}

	@SuppressWarnings("unchecked")
	public static <R extends ProcessingRecipe<?, ?>> List<RecipeHolder<R>> getRecipes(Level level, ItemStack item, RecipeType<R> type, Class<R> recipeClass, Predicate<? super RecipeHolder<R>> recipeFilter) {
		if (level == null || item.isEmpty())
			return List.of();

		List<RecipeHolder<R>> recipes = new ArrayList<>();
		for (RecipeHolder<?> holder : getLoadedRecipes(level)) {
			if (!(holder.value() instanceof SequencedAssemblyRecipe assembly))
				continue;
			if (holder.value().getType() != AllRecipeTypes.SEQUENCED_ASSEMBLY.getType())
				continue;
			if (!assembly.appliesTo(holder.id().identifier(), item))
				continue;

			ProcessingRecipe<?, ?> nextRecipe = assembly.getNextRecipe(item)
				.getRecipe();
			if (nextRecipe.getType() != type || !recipeClass.isInstance(nextRecipe))
				continue;

			R typedRecipe = (R) nextRecipe;
			RecipeHolder<R> typedHolder = new RecipeHolder<>(holder.id(), typedRecipe);
			if (!recipeFilter.test(typedHolder))
				continue;

			typedRecipe.enforceNextResult(() -> assembly.advance(holder.id().identifier(), item, level.getRandom()));
			recipes.add(typedHolder);
		}
		return recipes;
	}

	private static Collection<RecipeHolder<?>> getLoadedRecipes(Level level) {
		if (level.recipeAccess() instanceof RecipeManager recipeManager)
			return recipeManager.getRecipes();
		return List.of();
	}

	private ItemStack advance(Identifier id, ItemStack input, RandomSource random) {
		int step = getStep(input);
		if ((step + 1) / sequence.size() >= loops)
			return rollResult(random);

		ItemStack advancedItem = getTransitionalItem().copyWithCount(1);
		SequencedAssembly sequencedAssembly = new SequencedAssembly(
			id,
			step + 1,
			(step + 1f) / (sequence.size() * loops)
		);
		advancedItem.set(AllDataComponents.SEQUENCED_ASSEMBLY, sequencedAssembly);
		return advancedItem;
	}

	public int getLoops() {
		return loops;
	}

	private ItemStack rollResult(RandomSource random) {
		float totalWeight = 0;
		for (ProcessingOutput entry : resultPool)
			totalWeight += entry.getChance();
		float number = random.nextFloat() * totalWeight;
		for (ProcessingOutput entry : resultPool) {
			number -= entry.getChance();
			if (number < 0)
				return entry.getStack()
					.copy();
		}
		return ItemStack.EMPTY;
	}

	private boolean appliesTo(Identifier id, ItemStack input) {
		// First, check if the item is already in the middle of a sequenced assembly recipe
		if (input.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
			//noinspection DataFlowIssue
			return getTransitionalItem().getItem() == input.getItem() && input
				.get(AllDataComponents.SEQUENCED_ASSEMBLY)
				.id().equals(id);
		}
		// Else it must be the first step in a new sequenced assembly recipe
		return ingredient.test(input);
	}

	private SequencedRecipe<?> getNextRecipe(ItemStack input) {
		return sequence.get(getStep(input) % sequence.size());
	}

	private int getStep(ItemStack input) {
		if (!input.has(AllDataComponents.SEQUENCED_ASSEMBLY))
			return 0;
		//noinspection DataFlowIssue
		return input.get(AllDataComponents.SEQUENCED_ASSEMBLY).step();
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return false;
	}

	@Override
	public ItemStack assemble(RecipeWrapper input) {
		return ItemStack.EMPTY;
	}

	public boolean canCraftInDimensions(int width, int height) {
		return false;
	}

	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return resultPool.getFirst().getStack();
	}

	public float getOutputChance() {
		float totalWeight = 0;
		for (ProcessingOutput entry : resultPool)
			totalWeight += entry.getChance();
		return resultPool.getFirst().getChance() / totalWeight;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RecipeSerializer<? extends Recipe<RecipeWrapper>> getSerializer() {
		// Recipe serializers are registry values and must be returned by identity.
		// Creating a fresh wrapper here makes NeoForge unable to resolve its registry id
		// while synchronizing recipe content to a joining client.
		return AllRecipeTypes.SEQUENCED_ASSEMBLY.getSerializer();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RecipeType<? extends Recipe<RecipeWrapper>> getType() {
		return AllRecipeTypes.SEQUENCED_ASSEMBLY.getType();
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "sequenced_assembly";
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return SEQUENCED_ASSEMBLY_CATEGORY;
	}

	public static void addToTooltip(ItemTooltipEvent event) {
		SequencedAssemblyClient.addToTooltip(event);
	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	public List<SequencedRecipe<?>> getSequence() {
		return sequence;
	}

	public ItemStack getTransitionalItem() {
		return transitionalItem.getStack();
	}

	public record SequencedAssembly(Identifier id, int step, float progress) {
		public static final Codec<SequencedAssembly> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("id").forGetter(SequencedAssembly::id),
			Codec.INT.fieldOf("step").forGetter(SequencedAssembly::step),
			Codec.FLOAT.fieldOf("progress").forGetter(SequencedAssembly::progress)
		).apply(i, SequencedAssembly::new));

		public static final StreamCodec<ByteBuf, SequencedAssembly> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, SequencedAssembly::id,
			ByteBufCodecs.INT, SequencedAssembly::step,
			ByteBufCodecs.FLOAT, SequencedAssembly::progress,
			SequencedAssembly::new
		);
	}
}
