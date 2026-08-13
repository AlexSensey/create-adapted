package com.simibubi.create.content.equipment.goggles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Restores the old HEAD-only swap from the flat inventory icon to block/goggles. */
public class GogglesItemRenderer implements ItemModel {
	private final ItemModel iconModel;
	private final ItemModel wornModel;

	public GogglesItemRenderer(ItemModel iconModel, ItemModel wornModel) {
		this.iconModel = iconModel;
		this.wornModel = wornModel;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		ItemModel selected = displayContext == ItemDisplayContext.HEAD && wornModel != null ? wornModel : iconModel;
		selected.update(state, stack, resolver, displayContext, level, owner, seed);
	}
}
