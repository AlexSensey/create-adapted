package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BlockEntityWithoutLevelRenderer {
	public BlockEntityWithoutLevelRenderer(Object blockEntityRenderDispatcher, Object entityModelSet) {
	}

	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
		MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
	}
}
