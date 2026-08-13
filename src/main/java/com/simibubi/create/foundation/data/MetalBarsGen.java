package com.simibubi.create.foundation.data;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;

import java.util.function.Supplier;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.client.model.generators.ModelFile;

public class MetalBarsGen {

	public static <P extends IronBarsBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> barsBlockState(
		String name, boolean specialEdge) {
		return (c, p) -> {};
	}

	private static ModelFile barsSubModel(RegistrateBlockstateProvider p, String name, String suffix,
										  boolean specialEdge) {
		return new ModelFile.UncheckedModelFile("minecraft:block/air");
	}

	public static BlockEntry<IronBarsBlock> createBars(String name, boolean specialEdge,
													   Supplier<DataIngredient> ingredient, MapColor color) {
		return Create.registrate().block(name + "_bars", IronBarsBlock::new)
			.transform(CreateRegistrate.renderLayer("cutoutMipped"))
			.initialProperties(() -> Blocks.IRON_BARS)
			.properties(p -> p.sound(SoundType.COPPER)
				.mapColor(color))
			.tag(AllBlockTags.WRENCH_PICKUP.tag)
			.tag(AllBlockTags.FAN_TRANSPARENT.tag)
			.transform(TagGen.pickaxeOnly())
			.blockstate(barsBlockState(name, specialEdge))
			.item()
			.model((c, p) -> {})
			.recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 4))
			.build()
			.register();
	}

}
