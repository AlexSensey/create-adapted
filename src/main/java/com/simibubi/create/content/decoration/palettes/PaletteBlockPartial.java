package com.simibubi.create.content.decoration.palettes;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.Arrays;
import java.util.function.Supplier;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.createmod.catnip.api.lang.Lang;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

import net.neoforged.neoforge.client.model.generators.ModelFile;

public abstract class PaletteBlockPartial<B extends Block> {

	public static final PaletteBlockPartial<StairBlock> STAIR = new Stairs();
	public static final PaletteBlockPartial<SlabBlock> SLAB = new Slab(false);
	public static final PaletteBlockPartial<SlabBlock> UNIQUE_SLAB = new Slab(true);
	public static final PaletteBlockPartial<WallBlock> WALL = new Wall();

	public static final PaletteBlockPartial<?>[] ALL_PARTIALS = {STAIR, SLAB, WALL};
	public static final PaletteBlockPartial<?>[] FOR_POLISHED = {STAIR, UNIQUE_SLAB, WALL};

	private String name;

	private PaletteBlockPartial(String name) {
		this.name = name;
	}

	public @NonnullType BlockBuilder<B, CreateRegistrate> create(String variantName, PaletteBlockPattern pattern,
																 BlockEntry<? extends Block> block, AllPaletteStoneTypes variant) {
		String patternName = Lang.nonPluralId(pattern.createName(variantName));
		String blockName = patternName + "_" + this.name;

		BlockBuilder<B, CreateRegistrate> blockBuilder = Create.registrate()
			.block(blockName, p -> createBlock(block, blockName))
			.blockstate((c, p) -> generateBlockState(c, p, variantName, pattern, block))
			.recipe((c, p) -> createRecipes(variant, block, c, p))
			.transform(b -> transformBlock(b, variantName, pattern));

		ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> itemBuilder = blockBuilder.item()
			.transform(b -> transformItem(b, variantName, pattern));

		if (canRecycle())
			itemBuilder.tag(variant.materialTag);

		return itemBuilder.build();
	}

	protected Identifier getTexture(String variantName, PaletteBlockPattern pattern, int index) {
		return PaletteBlockPattern.toLocation(variantName, pattern.getTexture(index));
	}

	protected BlockBuilder<B, CreateRegistrate> transformBlock(BlockBuilder<B, CreateRegistrate> builder,
															   String variantName, PaletteBlockPattern pattern) {
		getBlockTags().forEach(builder::tag);
		return builder.transform(pickaxeOnly());
	}

	protected ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> transformItem(
		ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> builder, String variantName,
		PaletteBlockPattern pattern) {
		getItemTags().forEach(builder::tag);
		return builder;
	}

	protected boolean canRecycle() {
		return true;
	}

	protected abstract Iterable<TagKey<Block>> getBlockTags();

	protected abstract Iterable<TagKey<Item>> getItemTags();

	protected abstract B createBlock(Supplier<? extends Block> block, String blockName);

	protected Properties copiedProperties(Supplier<? extends Block> block, String blockName) {
		return Properties.ofFullCopy(block.get())
			.setId(ResourceKey.create(Registries.BLOCK, Create.asResource(blockName)));
	}

	protected abstract void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
										  DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p);

	protected abstract void generateBlockState(DataGenContext<Block, B> ctx, RegistrateBlockstateProvider prov,
											   String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block);

	private static class Stairs extends PaletteBlockPartial<StairBlock> {

		public Stairs() {
			super("stairs");
		}

		@Override
		protected StairBlock createBlock(Supplier<? extends Block> block, String blockName) {
			return new StairBlock(block.get().defaultBlockState(), copiedProperties(block, blockName));
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, StairBlock> ctx, RegistrateBlockstateProvider prov,
										  String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block) {
			// TODO 26.2: restore Registrate stair model generation.
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockTags.STAIRS);
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList();
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
									 DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.stairs(DataIngredient.items(patternBlock.get()), category, c::get, c.getName(), false);
			p.stonecutting(DataIngredient.tag(type.materialTag), category, c::get, 1);
		}

	}

	private static class Slab extends PaletteBlockPartial<SlabBlock> {

		private boolean customSide;

		public Slab(boolean customSide) {
			super("slab");
			this.customSide = customSide;
		}

		@Override
		protected SlabBlock createBlock(Supplier<? extends Block> block, String blockName) {
			return new SlabBlock(copiedProperties(block, blockName));
		}

		@Override
		protected boolean canRecycle() {
			return false;
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, SlabBlock> ctx, RegistrateBlockstateProvider prov,
										  String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block) {
			// TODO 26.2: restore Registrate slab model generation.
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockTags.SLABS);
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList();
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
									 DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.slab(DataIngredient.items(patternBlock.get()), category, c::get, c.getName(), false);
			p.stonecutting(DataIngredient.tag(type.materialTag), category, c::get, 2);
			// TODO 26.2: restore slab recycling recipe with new builder holder lookup.
		}

		@Override
		protected BlockBuilder<SlabBlock, CreateRegistrate> transformBlock(
			BlockBuilder<SlabBlock, CreateRegistrate> builder, String variantName, PaletteBlockPattern pattern) {
			builder.loot((lt, block) -> lt.add(block, lt.createSlabItemTable(block)));
			return super.transformBlock(builder, variantName, pattern);
		}

	}

	private static class Wall extends PaletteBlockPartial<WallBlock> {

		public Wall() {
			super("wall");
		}

		@Override
		protected WallBlock createBlock(Supplier<? extends Block> block, String blockName) {
			return new WallBlock(copiedProperties(block, blockName).forceSolidOn());
		}

		@Override
		protected ItemBuilder<BlockItem, BlockBuilder<WallBlock, CreateRegistrate>> transformItem(
			ItemBuilder<BlockItem, BlockBuilder<WallBlock, CreateRegistrate>> builder, String variantName,
			PaletteBlockPattern pattern) {
			// TODO 26.2: restore Registrate wall inventory model generation.
			return super.transformItem(builder, variantName, pattern);
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, WallBlock> ctx, RegistrateBlockstateProvider prov,
										  String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block) {
			// TODO 26.2: restore Registrate wall model generation.
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockTags.WALLS);
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList();
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
									 DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.stonecutting(DataIngredient.tag(type.materialTag), category, c::get, 1);
			// TODO 26.2: restore wall crafting recipe with new builder holder lookup.
		}

	}

}
