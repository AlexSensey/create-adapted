package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PartialItemModelRenderer {

	private static final PartialItemModelRenderer INSTANCE = new PartialItemModelRenderer();

	private ItemStack stack;
	private ItemDisplayContext transformType;
	private PoseStack ms;
	private MultiBufferSource buffer;
	private int overlay;

	public static PartialItemModelRenderer of(ItemStack stack, ItemDisplayContext transformType,
		PoseStack ms, MultiBufferSource buffer, int overlay) {
		PartialItemModelRenderer instance = INSTANCE;
		instance.stack = stack;
		instance.transformType = transformType;
		instance.ms = ms;
		instance.buffer = buffer;
		instance.overlay = overlay;
		return instance;
	}

	public void render(BakedModel model, int light) {
	}

	public void renderSolid(BakedModel model, int light) {
	}

	public void renderGlowing(BakedModel model, int light) {
	}

	public void renderSolidGlowing(BakedModel model, int light) {
	}

	public void render(BakedModel model, RenderType type, int light) {
	}
}
