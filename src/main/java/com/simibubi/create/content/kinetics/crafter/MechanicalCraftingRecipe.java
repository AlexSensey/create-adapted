package com.simibubi.create.content.kinetics.crafter;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

public class MechanicalCraftingRecipe extends CustomRecipe {
	private final String group;
	private final CraftingBookCategory category;
	private final boolean acceptMirrored;
	private final ShapedRecipePattern pattern;
	private final ItemStackTemplate result;

	public MechanicalCraftingRecipe(String groupIn, CraftingBookCategory category,
		ShapedRecipePattern pattern, ItemStack recipeOutputIn, boolean acceptMirrored) {
		this(groupIn, category, pattern, ItemStackTemplate.fromStack(recipeOutputIn), acceptMirrored);
	}

	private MechanicalCraftingRecipe(String groupIn, CraftingBookCategory category,
		ShapedRecipePattern pattern, ItemStackTemplate recipeOutputIn, boolean acceptMirrored) {
		super();
		this.group = groupIn;
		this.category = category;
		this.acceptMirrored = acceptMirrored;
		this.pattern = pattern;
		this.result = recipeOutputIn;
	}

	@Override
	public boolean matches(CraftingInput input, Level worldIn) {
		if (!(input instanceof MechanicalCraftingInput) || pattern == null)
			return false;
		if (acceptsMirrored())
			return pattern.matches(input);

		for (int x = 0; x <= input.width() - pattern.width(); x++)
			for (int y = 0; y <= input.height() - pattern.height(); y++)
				if (matchesSpecific(input, x, y))
					return true;
		return false;
	}

	private boolean matchesSpecific(CraftingInput input, int offsetX, int offsetY) {
		List<Optional<Ingredient>> ingredients = pattern.ingredients();
		int width = pattern.width();
		int height = pattern.height();

		for (int x = 0; x < input.width(); x++) {
			for (int y = 0; y < input.height(); y++) {
				int recipeX = x - offsetX;
				int recipeY = y - offsetY;
				Optional<Ingredient> ingredient = Optional.empty();
				if (recipeX >= 0 && recipeY >= 0 && recipeX < width && recipeY < height)
					ingredient = ingredients.get(recipeX + recipeY * width);
				if (!Ingredient.testOptionalIngredient(ingredient, input.getItem(x + y * input.width())))
					return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return result.create();
	}

	@Override
	public String group() {
		return group;
	}

	@Override
	public CraftingBookCategory category() {
		return category;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public @NotNull RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return AllRecipeTypes.MECHANICAL_CRAFTING.getSerializer();
	}

	public boolean acceptsMirrored() {
		return acceptMirrored;
	}

	public ShapedRecipePattern getPattern() {
		return pattern;
	}

	public ItemStack getResultStack() {
		return result.create();
	}

	private ShapedRecipePattern pattern() {
		return pattern;
	}

	private ItemStackTemplate resultTemplate() {
		return result;
	}

	public static class Serializer {
		public static final MapCodec<MechanicalCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.optionalFieldOf("group", "").forGetter(MechanicalCraftingRecipe::group),
			CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
				.forGetter(MechanicalCraftingRecipe::category),
			ShapedRecipePattern.MAP_CODEC.forGetter(MechanicalCraftingRecipe::pattern),
			ItemStackTemplate.MAP_CODEC.fieldOf("result").forGetter(MechanicalCraftingRecipe::resultTemplate),
			Codec.BOOL.optionalFieldOf("accept_mirrored", true).forGetter(MechanicalCraftingRecipe::acceptsMirrored)
		).apply(instance, (group, category, pattern, result, acceptMirrored) ->
			new MechanicalCraftingRecipe(group, category, pattern, result, acceptMirrored)));

		public static final StreamCodec<RegistryFriendlyByteBuf, MechanicalCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, MechanicalCraftingRecipe::group,
			CraftingBookCategory.STREAM_CODEC, MechanicalCraftingRecipe::category,
			ShapedRecipePattern.STREAM_CODEC, MechanicalCraftingRecipe::pattern,
			ItemStackTemplate.STREAM_CODEC, MechanicalCraftingRecipe::resultTemplate,
			ByteBufCodecs.BOOL, MechanicalCraftingRecipe::acceptsMirrored,
			MechanicalCraftingRecipe::new
		);

		public @NotNull MapCodec<MechanicalCraftingRecipe> codec() {
			return CODEC;
		}

		public @NotNull StreamCodec<RegistryFriendlyByteBuf, MechanicalCraftingRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		public RecipeSerializer<MechanicalCraftingRecipe> asRecipeSerializer() {
			return new RecipeSerializer<>(CODEC, STREAM_CODEC);
		}
	}
}
