package com.simibubi.create.foundation.data;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.simibubi.create.content.contraptions.chassis.LinearChassisBlock;
import com.simibubi.create.content.contraptions.chassis.RadialChassisBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class BlockStateGen {

	private static final NonNullBiConsumer<DataGenContext<Block, Block>, RegistrateBlockstateProvider> NOOP = ($, $$) -> {};

	@SuppressWarnings("unchecked")
	private static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> noop() {
		return (NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider>) (NonNullBiConsumer<?, ?>) NOOP;
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> axisBlockProvider(
		boolean customItem) {
		return noop();
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> directionalBlockProvider(
		boolean customItem) {
		return noop();
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> directionalBlockProviderIgnoresWaterlogged(
		boolean customItem) {
		return noop();
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalBlockProvider(
		boolean customItem) {
		return noop();
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalAxisBlockProvider(
		boolean customItem) {
		return noop();
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleCubeAll(
		String path) {
		return noop();
	}

	public static <T extends DirectionalAxisKineticBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> directionalAxisBlockProvider() {
		return noop();
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalWheelProvider(
		boolean customItem) {
		return noop();
	}

	public static <T extends Block> void directionalBlockIgnoresWaterlogged(DataGenContext<Block, T> ctx,
		RegistrateBlockstateProvider prov, Function<BlockState, ModelFile> modelFunc) {}

	public static <T extends Block> void axisBlock(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		Function<BlockState, ModelFile> modelFunc) {}

	public static <T extends Block> void axisBlock(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		Function<BlockState, ModelFile> modelFunc, boolean uvLock) {}

	public static <T extends Block> void simpleBlock(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		Function<BlockState, ModelFile> modelFunc) {}

	public static <T extends Block> void horizontalAxisBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockstateProvider prov, Function<BlockState, ModelFile> modelFunc) {}

	public static <T extends DirectionalAxisKineticBlock> void directionalAxisBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockstateProvider prov, BiFunction<BlockState, Boolean, ModelFile> modelFunc) {}

	public static <T extends Block> void horizontalWheel(DataGenContext<Block, T> ctx,
		RegistrateBlockstateProvider prov, Function<BlockState, ModelFile> modelFunc) {}

	public static <T extends Block> void cubeAll(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		String textureSubDir) {}

	public static <T extends Block> void cubeAll(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		String textureSubDir, String name) {}

	public static NonNullBiConsumer<DataGenContext<Block, SchematicannonBlock>, RegistrateBlockstateProvider> schematicannon() {
		return noop();
	}

	public static NonNullBiConsumer<DataGenContext<Block, CartAssemblerBlock>, RegistrateBlockstateProvider> cartAssembler() {
		return noop();
	}

	public static NonNullBiConsumer<DataGenContext<Block, BlazeBurnerBlock>, RegistrateBlockstateProvider> blazeHeater() {
		return noop();
	}

	public static <B extends LinearChassisBlock> NonNullBiConsumer<DataGenContext<Block, B>, RegistrateBlockstateProvider> linearChassis() {
		return noop();
	}

	public static <B extends RadialChassisBlock> NonNullBiConsumer<DataGenContext<Block, B>, RegistrateBlockstateProvider> radialChassis() {
		return noop();
	}

	public static <P extends Block> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> naturalStoneTypeBlock(
		String type) {
		return noop();
	}

	public static <P extends EncasedPipeBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> encasedPipe() {
		return noop();
	}

	public static <P extends TrapDoorBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> uvLockedTrapdoorBlock(
		P block, ModelFile bottom, ModelFile top, ModelFile open) {
		return noop();
	}

	public static <P extends WhistleExtenderBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> whistleExtender() {
		return noop();
	}

	public static <P extends FluidPipeBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> pipe() {
		return noop();
	}

	public static Function<BlockState, ConfiguredModel[]> mapToAir(@NonnullType RegistrateBlockstateProvider p) {
		return $ -> new ConfiguredModel[0];
	}

}
