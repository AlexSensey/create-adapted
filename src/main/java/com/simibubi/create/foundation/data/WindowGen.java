package com.simibubi.create.foundation.data;

import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;

import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.GlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.WindowBlock;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.GlassPaneCTBehaviour;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;

public class WindowGen {
	private static final CreateRegistrate REGISTRATE = Create.registrate();

	private static Properties glassProperties(Properties p) {
		return p.isValidSpawn(WindowGen::never)
			.isRedstoneConductor(WindowGen::never)
			.isSuffocating(WindowGen::never)
			.isViewBlocking(WindowGen::never);
	}

	private static boolean never(BlockState p_235436_0_, BlockGetter p_235436_1_, BlockPos p_235436_2_) {
		return false;
	}

	private static Boolean never(BlockState p_235427_0_, BlockGetter p_235427_1_, BlockPos p_235427_2_,
								 EntityType<?> p_235427_3_) {
		return false;
	}

	public static BlockEntry<WindowBlock> woodenWindowBlock(WoodType woodType, Block planksBlock) {
		return woodenWindowBlock(woodType, planksBlock, "cutoutMipped", false);
	}

	public static BlockBuilder<WindowBlock, CreateRegistrate> randomisedWindowBlock(String name,
																					Supplier<? extends ItemLike> ingredient, String renderLayer, boolean translucent,
																					Supplier<MapColor> color) {
		Identifier end_texture = Create.asResource(palettesDir() + name + "_end");
		Identifier side_texture = Create.asResource(palettesDir() + name);
		Function<Integer, Identifier> ends = i -> Create.asResource(palettesDir() + name + "_" + i + "_end");
		return windowBlock(name, ingredient, null, renderLayer, translucent, n -> end_texture, n -> side_texture, color)
			.blockstate((c, p) -> {
			});
	}

	public static BlockEntry<WindowBlock> customWindowBlock(String name, Supplier<? extends ItemLike> ingredient,
															Supplier<CTSpriteShiftEntry> ct, String renderLayer, boolean translucent,
															Supplier<MapColor> color) {
		NonNullFunction<String, Identifier> end_texture = n -> Create.asResource(palettesDir() + name + "_end");
		NonNullFunction<String, Identifier> side_texture = n -> Create.asResource(palettesDir() + n);
		return windowBlock(name, ingredient, ct, renderLayer, translucent, end_texture, side_texture, color).register();
	}

	public static BlockEntry<WindowBlock> woodenWindowBlock(WoodType woodType, Block planksBlock,
															String renderLayer, boolean translucent) {
		String woodName = woodType.name();
		String name = woodName + "_window";
		NonNullFunction<String, Identifier> end_texture =
			$ -> Identifier.withDefaultNamespace("block/" + woodName + "_planks");
		NonNullFunction<String, Identifier> side_texture = n -> Create.asResource(palettesDir() + n);
		return windowBlock(name, () -> planksBlock, () -> AllSpriteShifts.getWoodenWindow(woodType), renderLayer,
			translucent, end_texture, side_texture, planksBlock::defaultMapColor).register();
	}

	public static BlockBuilder<WindowBlock, CreateRegistrate> windowBlock(String name,
																		  Supplier<? extends ItemLike> ingredient, Supplier<CTSpriteShiftEntry> ct,
																		  String renderLayer, boolean translucent,
																		  NonNullFunction<String, Identifier> endTexture, NonNullFunction<String, Identifier> sideTexture,
																		  Supplier<MapColor> color) {
		return REGISTRATE.block(name, p -> new WindowBlock(p, translucent))
			.onRegister(ct == null ? $ -> {
			} : connectedTextures(() -> new HorizontalCTBehaviour(ct.get())))
			.transform(CreateRegistrate.renderLayer(renderLayer))
			.initialProperties(() -> Blocks.GLASS)
			.properties(WindowGen::glassProperties)
			.properties(p -> p.mapColor(color.get()))
			.loot((t, g) -> t.dropWhenSilkTouch(g))
			.blockstate((c, p) -> {
			})
			.tag(BlockTags.IMPERMEABLE)
			.simpleItem();
	}

	public static BlockEntry<ConnectedGlassBlock> framedGlass(String name,
															  Supplier<ConnectedTextureBehaviour> behaviour) {
		return REGISTRATE.block(name, ConnectedGlassBlock::new)
			.onRegister(connectedTextures(behaviour))
			.transform(CreateRegistrate.renderLayer("cutout"))
			.initialProperties(() -> Blocks.GLASS)
			.properties(WindowGen::glassProperties)
			.loot((t, g) -> t.dropWhenSilkTouch(g))
				.recipe((c, p) -> p.stonecutting(DataIngredient.tag(Tags.Items.GLASS_BLOCKS_COLORLESS),
				RecipeCategory.BUILDING_BLOCKS, c::get))
			.blockstate((c, p) -> BlockStateGen.cubeAll(c, p, "palettes/", "framed_glass"))
				.tag(Tags.Blocks.GLASS_BLOCKS_COLORLESS, BlockTags.IMPERMEABLE)
			.item()
				.tag(Tags.Items.GLASS_BLOCKS_COLORLESS)
			.build()
			.register();
	}

	public static BlockEntry<ConnectedGlassPaneBlock> framedGlassPane(String name, Supplier<? extends Block> parent,
																	  Supplier<CTSpriteShiftEntry> ctshift) {
		Identifier sideTexture = Create.asResource(palettesDir() + "framed_glass");
		Identifier itemSideTexture = Create.asResource(palettesDir() + name);
		Identifier topTexture = Create.asResource(palettesDir() + "framed_glass_pane_top");
		return connectedGlassPane(name, parent, ctshift, sideTexture, itemSideTexture, topTexture, "cutoutMipped", true)
			.register();
	}

	public static BlockBuilder<ConnectedGlassPaneBlock, CreateRegistrate> customWindowPane(String name,
																						   Supplier<? extends Block> parent, Supplier<CTSpriteShiftEntry> ctshift,
																						   String renderLayer) {
		Identifier topTexture = Create.asResource(palettesDir() + name + "_pane_top");
		Identifier sideTexture = Create.asResource(palettesDir() + name);
		return connectedGlassPane(name, parent, ctshift, sideTexture, sideTexture, topTexture, renderLayer, false);
	}

	public static BlockEntry<ConnectedGlassPaneBlock> woodenWindowPane(WoodType woodType,
																	   Supplier<? extends Block> parent) {
		return woodenWindowPane(woodType, parent, "cutoutMipped");
	}

	public static BlockEntry<ConnectedGlassPaneBlock> woodenWindowPane(WoodType woodType,
																	   Supplier<? extends Block> parent, String renderLayer) {
		String woodName = woodType.name();
		String name = woodName + "_window";
		Identifier topTexture = Identifier.withDefaultNamespace("block/" + woodName + "_planks");
		Identifier sideTexture = Create.asResource(palettesDir() + name);
		return connectedGlassPane(name, parent, () -> AllSpriteShifts.getWoodenWindow(woodType), sideTexture,
			sideTexture, topTexture, renderLayer, false).register();
	}

	public static BlockEntry<GlassPaneBlock> standardGlassPane(String name, Supplier<? extends Block> parent,
															   Identifier sideTexture, Identifier topTexture, String renderLayer) {
		NonNullBiConsumer<DataGenContext<Block, GlassPaneBlock>, RegistrateBlockstateProvider> stateProvider =
			(c, p) -> {
			};
		return glassPane(name, parent, sideTexture, topTexture, GlassPaneBlock::new, renderLayer, $ -> {
		}, stateProvider, true).register();
	}

	private static BlockBuilder<ConnectedGlassPaneBlock, CreateRegistrate> connectedGlassPane(String name,
																							  Supplier<? extends Block> parent, Supplier<CTSpriteShiftEntry> ctshift, Identifier sideTexture,
																							  Identifier itemSideTexture, Identifier topTexture, String renderLayer, boolean colorless) {
		NonNullConsumer<? super ConnectedGlassPaneBlock> connectedTextures = ctshift == null ? $ -> {
		} : connectedTextures(() -> new GlassPaneCTBehaviour(ctshift.get()));
		String CGPparents = "block/connected_glass_pane/";
		String prefix = name + "_pane_";

		Function<RegistrateBlockstateProvider, ModelFile> post =
			getPaneModelProvider(CGPparents, prefix, "post", sideTexture, topTexture),
			side = getPaneModelProvider(CGPparents, prefix, "side", sideTexture, topTexture),
			sideAlt = getPaneModelProvider(CGPparents, prefix, "side_alt", sideTexture, topTexture),
			noSide = getPaneModelProvider(CGPparents, prefix, "noside", sideTexture, topTexture),
			noSideAlt = getPaneModelProvider(CGPparents, prefix, "noside_alt", sideTexture, topTexture);

		NonNullBiConsumer<DataGenContext<Block, ConnectedGlassPaneBlock>, RegistrateBlockstateProvider> stateProvider =
			(c, p) -> {
			};

		return glassPane(name, parent, itemSideTexture, topTexture, ConnectedGlassPaneBlock::new, renderLayer,
			connectedTextures, stateProvider, colorless);
	}

	private static Function<RegistrateBlockstateProvider, ModelFile> getPaneModelProvider(String CGPparents,
																						  String prefix, String partial, Identifier sideTexture, Identifier topTexture) {
		return p -> p.models()
			.withExistingParent(prefix + partial, Create.asResource(CGPparents + partial))
			.texture("pane", sideTexture)
			.texture("edge", topTexture);
	}

	private static <G extends GlassPaneBlock> BlockBuilder<G, CreateRegistrate> glassPane(String name,
																						  Supplier<? extends Block> parent, Identifier sideTexture, Identifier topTexture,
																						  NonNullFunction<Properties, G> factory, String renderLayer,
																						  NonNullConsumer<? super G> connectedTextures,
																						  NonNullBiConsumer<DataGenContext<Block, G>, RegistrateBlockstateProvider> stateProvider, boolean colorless) {
		name += "_pane";


		ItemBuilder<BlockItem, BlockBuilder<G, CreateRegistrate>> itemBuilder = REGISTRATE.block(name, factory)
			.onRegister(connectedTextures)
			.transform(CreateRegistrate.renderLayer(renderLayer))
			.initialProperties(() -> Blocks.GLASS_PANE)
			.properties(p -> p.mapColor(parent.get()
				.defaultMapColor()))
			.blockstate(stateProvider)
			.loot((t, g) -> t.dropWhenSilkTouch(g))
			.item();

		if (colorless)
			itemBuilder.tag(Tags.Items.GLASS_PANES, Tags.Items.GLASS_PANES_COLORLESS);
		else
			itemBuilder.tag(Tags.Items.GLASS_PANES);

		BlockBuilder<G, CreateRegistrate> blockBuilder = itemBuilder
			.build();

		if (colorless)
			blockBuilder.tag(Tags.Blocks.GLASS_PANES, Tags.Blocks.GLASS_PANES_COLORLESS);
		else
			blockBuilder.tag(Tags.Blocks.GLASS_PANES);

		return blockBuilder;
	}

	private static String palettesDir() {
		return "block/palettes/";
	}

}
