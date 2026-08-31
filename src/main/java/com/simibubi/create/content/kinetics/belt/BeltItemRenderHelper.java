package com.simibubi.create.content.kinetics.belt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Client-only model classification shared by belts and depots. */
public final class BeltItemRenderHelper {

	private BeltItemRenderHelper() {}

	public static ItemStackRenderState createRenderState(ItemStack stack) {
		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, null, null, 0);
		return itemState;
	}

	/** Minecraft 26.2 equivalent of the old baked-model {@code isGui3d()} flag. */
	public static boolean isGui3d(ItemStackRenderState itemState) {
		return itemState.usesBlockLight();
	}
}
