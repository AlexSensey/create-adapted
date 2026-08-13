package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public abstract class CustomRenderedItemModelRenderer extends BlockEntityWithoutLevelRenderer {

	public CustomRenderedItemModelRenderer() {
		super(null, null);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (stack.isEmpty())
			return;

		CustomRenderedItemModel mainModel = CustomRenderedItems.getModel(stack.getItem());
		if (mainModel == null)
			return;

		PartialItemModelRenderer renderer = PartialItemModelRenderer.of(stack, transformType, ms, buffer, overlay);

		ms.pushPose();
		ms.translate(0.5F, 0.5F, 0.5F);
		render(stack, mainModel, renderer, transformType, ms, buffer, light, overlay);
		ms.popPose();
	}

	protected abstract void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
		PoseStack ms, MultiBufferSource buffer, int light, int overlay);

}
