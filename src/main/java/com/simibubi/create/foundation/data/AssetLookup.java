package com.simibubi.create.foundation.data;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;

import java.util.function.Function;

public class AssetLookup {
	private static final ModelFile DUMMY_MODEL = new UncheckedModelFile("minecraft:block/air");

	/**
	 * Custom block models packaged with other partials. Example:
	 * models/block/schematicannon/block.json <br>
	 * <br>
	 * Adding "powered", "vertical" will look for /block_powered_vertical.json
	 */
	public static ModelFile partialBaseModel(DataGenContext<?, ?> ctx, RegistrateBlockstateProvider prov,
		String... suffix) {
		return DUMMY_MODEL;
	}

	/**
	 * Custom block model from models/block/x.json
	 */
	public static ModelFile standardModel(DataGenContext<?, ?> ctx, RegistrateBlockstateProvider prov) {
		return DUMMY_MODEL;
	}

	/**
	 * Generate item model inheriting from a seperate model in
	 * models/block/x/item.json
	 */
	public static <I extends BlockItem> ItemModelBuilder customItemModel(DataGenContext<Item, I> ctx,
		RegistrateItemModelProvider prov) {
		return null;
	}

	/**
	 * Generate item model inheriting from a seperate model in
	 * models/block/folders[0]/folders[1]/.../item.json "_" will be replaced by the
	 * item name
	 */
	public static <I extends BlockItem> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelProvider> customBlockItemModel(
		String... folders) {
		return (c, p) -> {};
	}

	public static <I extends Item> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelProvider> customGenericItemModel(
		String... folders) {
		return (c, p) -> {};
	}

	public static Function<BlockState, ModelFile> forPowered(DataGenContext<?, ?> ctx,
		RegistrateBlockstateProvider prov) {
		return state -> DUMMY_MODEL;
	}

	public static Function<BlockState, ModelFile> forPowered(DataGenContext<?, ?> ctx,
		RegistrateBlockstateProvider prov, String path) {
		return state -> DUMMY_MODEL;
	}

	public static Function<BlockState, ModelFile> withIndicator(DataGenContext<?, ?> ctx,
		RegistrateBlockstateProvider prov, Function<BlockState, ModelFile> baseModelFunc, IntegerProperty property) {
		return state -> DUMMY_MODEL;
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> existingItemModel() {
		return (c, p) -> {};
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemModel(String name) {
		return (c, p) -> {};
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemModelWithPartials() {
		return (c, p) -> {};
	}

}
