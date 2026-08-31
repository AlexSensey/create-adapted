package net.minecraft.client.renderer.block;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class BlockModelShaper {
	public static ModelResourceLocation stateToModelLocation(Identifier id, BlockState state) {
		return new ModelResourceLocation(id, "normal");
	}
}
