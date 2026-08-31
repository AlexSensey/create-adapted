package net.minecraft.client.renderer.block;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockRenderDispatcher {
	private final ModelBlockRenderer modelRenderer = new ModelBlockRenderer(false, false, null);

	public ModelBlockRenderer getModelRenderer() {
		return modelRenderer;
	}

	public BakedModel getBlockModel(BlockState state) {
		return null;
	}
}
