package com.simibubi.create.foundation.data.recipe;

import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.CuttingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider.I;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;

/**
 * Create's own Data Generation for Cutting recipes
 * @see CuttingRecipeGen
 */
@SuppressWarnings("unused")
public final class CreateCuttingRecipeGen extends CuttingRecipeGen {

	GeneratedRecipe
		ANDESITE_ALLOY = create(I::andesiteAlloy, b -> b.duration(200)
			.output(AllBlocks.SHAFT.get(), 6)),

		OAK_LOG = stripAndMakePlanks(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS),
		OAK_WOOD = stripAndMakePlanks(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD, Blocks.OAK_PLANKS),
		SPRUCE_LOG = stripAndMakePlanks(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS),
		SPRUCE_WOOD = stripAndMakePlanks(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.SPRUCE_PLANKS),
		BIRCH_LOG = stripAndMakePlanks(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_PLANKS),
		BIRCH_WOOD = stripAndMakePlanks(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD, Blocks.BIRCH_PLANKS),
		JUNGLE_LOG = stripAndMakePlanks(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_PLANKS),
		JUNGLE_WOOD = stripAndMakePlanks(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.JUNGLE_PLANKS),
		ACACIA_LOG = stripAndMakePlanks(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_PLANKS),
		ACACIA_WOOD = stripAndMakePlanks(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.ACACIA_PLANKS),
		DARK_OAK_LOG = stripAndMakePlanks(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS),
		DARK_OAK_WOOD = stripAndMakePlanks(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD,
			Blocks.DARK_OAK_PLANKS),
		MANGROVE_LOG = stripAndMakePlanks(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG, Blocks.MANGROVE_PLANKS),
		MANGROVE_WOOD = stripAndMakePlanks(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD,
			Blocks.MANGROVE_PLANKS),
		CHERRY_LOG = stripAndMakePlanks(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_PLANKS),
		CHERRY_WOOD = stripAndMakePlanks(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD, Blocks.CHERRY_PLANKS),
		CRIMSON_STEM = stripAndMakePlanks(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_PLANKS),
		CRIMSON_HYPHAE = stripAndMakePlanks(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE,
			Blocks.CRIMSON_PLANKS),
		WARPED_STEM = stripAndMakePlanks(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_PLANKS),
		WARPED_HYPHAE = stripAndMakePlanks(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE,
			Blocks.WARPED_PLANKS),

		BAMBOO_PLANKS = create(() -> Blocks.BAMBOO_PLANKS, b -> b.duration(20)
			.output(Blocks.BAMBOO_MOSAIC, 1)),

	/*
	 * Mod compat
	 */

		// Ars Nouveau (all logs yield the same plank) (blue is covered by RuntimeDataGenerator to handle the planks into other recipes)
		ARS_N_1 = stripAndMakePlanks(Mods.ARS_N, null, "stripped_purple_archwood_log", "archwood_planks"),
		ARS_N_2 = stripAndMakePlanks(Mods.ARS_N, null, "stripped_green_archwood_log", "archwood_planks"),
		ARS_N_3 = stripAndMakePlanks(Mods.ARS_N, null, "stripped_red_archwood_log", "archwood_planks"),
		ARS_N_4 = stripAndMakePlanks(Mods.ARS_N, null, "stripped_purple_archwood_wood", "archwood_planks"),
		ARS_N_5 = stripAndMakePlanks(Mods.ARS_N, null, "stripped_green_archwood_wood", "archwood_planks"),
		ARS_N_6 = stripAndMakePlanks(Mods.ARS_N, null, "stripped_red_archwood_wood", "archwood_planks"),

		// Ars Elemental
		ARS_E_1 = stripAndMakePlanksDiffPlanksModId(Mods.ARS_E, null, "stripped_yellow_archwood_log", Mods.ARS_N, "archwood_planks"),
		ARS_E_2 = stripAndMakePlanksDiffPlanksModId(Mods.ARS_E, null, "stripped_yellow_archwood", Mods.ARS_N, "archwood_planks"),

		// Regions Unexplored
		RU_1 = stripAndMakePlanks(Mods.RU, "brimwood_log_magma", "stripped_brimwood_log", null),
		RU_2 = stripAndMakePlanks(Mods.RU, "ashen_log", "stripped_dead_log", null),
		RU_3 = stripAndMakePlanks(Mods.RU, "ashen_wood", "stripped_dead_wood", null),
		RU_4 = stripOnlyDiffModId(Mods.RU, "silver_birch_log", Mods.MC, "stripped_birch_log"),
		RU_5 = stripOnlyDiffModId(Mods.RU, "silver_birch_wood", Mods.MC, "stripped_birch_wood"),

		// Autumnity
		AUTUM_1 = stripAndMakePlanks(Mods.AUTUM, null, "sappy_maple_log", "maple_planks"),
		AUTUM_2 = stripAndMakePlanks(Mods.AUTUM, null, "sappy_maple_wood", "maple_planks"),

		// Endergetic Expansion
		ENDERGETIC_1 = stripAndMakePlanks(Mods.ENDER, "glowing_poise_stem", "stripped_poise_stem", null),
		ENDERGETIC_2 = stripAndMakePlanks(Mods.ENDER, "glowing_poise_wood", "stripped_poise_wood", null),

		// IE
		IE_WIRES = ieWires(CommonMetal.COPPER, CommonMetal.ELECTRUM, CommonMetal.ALUMINUM, CommonMetal.STEEL, CommonMetal.LEAD),

		// Jaden's Nether Expansion
		JNE_1 = stripAndMakePlanks(Mods.JNE, "cerebrage_claret_stem", "stripped_claret_stem", null),
		JNE_2 = stripAndMakePlanks(Mods.JNE, "cerebrage_claret_hyphae", "stripped_claret_hyphae", null),

		// Atmospheric
		ATM_1 = stripAndMakePlanks(Mods.ATM, "watchful_aspen_log", "aspen_log", null),
	    ATM_2 = stripAndMakePlanks(Mods.ATM, "watchful_aspen_wood", "aspen_wood", null),
		ATM_3 = stripAndMakePlanks(Mods.ATM, "crustose_log", "aspen_log", null),
		ATM_4 = stripAndMakePlanks(Mods.ATM, "crustose_wood", "aspen_wood", null),

		// Oh The Biomes We've Gone
		BWG_1 = stripAndMakePlanksDiffPlanksModId(Mods.BWG, null, "stripped_palo_verde_log", Mods.VANILLA, "birch_planks"),
		BWG_2 = stripAndMakePlanksDiffPlanksModId(Mods.BWG, null, "stripped_palo_verde_wood", Mods.VANILLA, "birch_planks")
		;

	public CreateCuttingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
		super(output, registries, Create.ID);
	}

	public CreateCuttingRecipeGen(Provider registries, net.minecraft.data.recipes.RecipeOutput output) {
		super(registries, output, Create.ID);
	}

	GeneratedRecipe ieWires(CommonMetal... metals) {
		for (CommonMetal metal : metals)
			create(Mods.IE.recipeId("wire_" + metal), b -> b.duration(50)
				.require(metal.plates)
				.output(1, Mods.IE, "wire_" + metal, 2)
				.whenModLoaded(Mods.IE.getId()));
		return null;
	}
}
