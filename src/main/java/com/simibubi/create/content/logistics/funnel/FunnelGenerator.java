package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class FunnelGenerator extends SpecialBlockStateGen {

	private String type;
	private Identifier blockTexture;
	private boolean hasFilter;

	public FunnelGenerator(String type, boolean hasFilter) {
		this.type = type;
		this.hasFilter = hasFilter;
		this.blockTexture = Create.asResource("block/" + type + "_block");
	}

	@Override
	protected int getXRotation(BlockState state) {
		return state.getValue(FunnelBlock.FACING) == Direction.DOWN ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(FunnelBlock.FACING)) + 180;
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> c, RegistrateBlockstateProvider p,
		BlockState s) {
		return p.models()
			.getExistingFile(Identifier.withDefaultNamespace("block/air"));
	}

	public static NonNullBiConsumer<DataGenContext<Item, FunnelItem>, RegistrateItemModelProvider> itemModel(
		String type) {
		return (c, p) -> {};
	}

}
