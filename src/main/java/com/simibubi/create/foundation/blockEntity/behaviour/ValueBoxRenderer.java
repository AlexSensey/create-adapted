package com.simibubi.create.foundation.blockEntity.behaviour;

import org.joml.Matrix3f;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.Minecraft;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ValueBoxRenderer {

	public static void renderItemIntoValueBox(ItemStack filter, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
	}

	public static void renderFlatItemIntoValueBox(ItemStack filter, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
	}

	public static void submitItemIntoValueBox(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		submitItemIntoValueBox(filter, ms, collector, light, .24f);
	}

	public static void submitItemIntoValueBox(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
		int light, float scale) {
		submitItemIntoValueBox(filter, ms, collector, light, scale, 1 / 128f);
	}

	public static void submitItemIntoValueBox(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
		int light, float scale, float surfaceOffset) {
		if (filter.isEmpty())
			return;

		float fixedScale = filter.getItem() instanceof BlockItem ? scale * 4 : scale * 2;
		ms.pushPose();
		ms.translate(0, 0, surfaceOffset);
		ms.scale(fixedScale, fixedScale, fixedScale);

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, filter, ItemDisplayContext.FIXED, null, null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public static void submitFlatItemIntoValueBox(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		if (filter.isEmpty())
			return;

		int blockLight = light >> 4 & 0xf;
		int skyLight = light >> 20 & 0xf;
		int itemLight = Mth.floor(skyLight + .5f) << 20 | (Mth.floor(blockLight + .5f) & 0xf) << 4;

		ms.pushPose();
		TransformStack.of(ms)
			.rotateXDegrees(230);
		Matrix3f normal = new Matrix3f(ms.last()
			.normal());
		ms.popPose();

		ms.pushPose();
		TransformStack.of(ms)
			.translate(0, 0, -1 / 4f)
			.translate(0, 0, 1 / 32f + .001)
			.rotateYDegrees(180);

		PoseStack squashed = new PoseStack();
		squashed.last()
			.pose()
			.mul(ms.last()
				.pose());
		squashed.scale(.5f, .5f, 1 / 1024f);
		squashed.last()
			.normal()
			.set(normal);

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, filter, ItemDisplayContext.GUI, null, null, 0);
		itemState.submit(squashed, collector, itemLight, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}
}
