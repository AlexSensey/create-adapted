package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FlatGuiItemRenderer {
	private static final float DEPTH_SCALE = 1 / 1024f;
	private static final float SURFACE_OFFSET = 1 / 128f;

	public static void submit(ItemStack stack, PoseStack ms, SubmitNodeCollector collector, int light, float scale) {
		submit(stack, ms, collector, light, scale, 0);
	}

	public static void submit(ItemStack stack, PoseStack ms, SubmitNodeCollector collector, int light, float scale,
		float zRotationDegrees) {
		submit(stack, ms, collector, light, scale, zRotationDegrees, SURFACE_OFFSET);
	}

	public static void submit(ItemStack stack, PoseStack ms, SubmitNodeCollector collector, int light, float scale,
		float zRotationDegrees, float surfaceOffset) {
		if (stack.isEmpty())
			return;

		int blockLight = light >> 4 & 0xf;
		int skyLight = light >> 20 & 0xf;
		int itemLight = Mth.floor(skyLight + .5f) << 20 | (Mth.floor(blockLight + .5f) & 0xf) << 4;

		ms.pushPose();
		ms.translate(0, 0, surfaceOffset);
		if (zRotationDegrees != 0)
			ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(zRotationDegrees));

		PoseStack squashed = new PoseStack();
		squashed.last()
			.pose()
			.mul(ms.last()
				.pose());
		squashed.scale(scale, scale, DEPTH_SCALE);
		squashed.last()
			.normal()
			.set(ms.last()
				.normal());

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, stack, ItemDisplayContext.GUI, null, null, 0);
		itemState.submit(squashed, collector, itemLight, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}
}
