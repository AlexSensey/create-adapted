package com.simibubi.create.content.equipment.tool;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 26.2 replacement for the former custom cardboard sword renderer. */
public class CardboardSwordItemRenderer implements ItemModel {

	private final ItemModel inventoryModel;
	private final ItemModel heldModel;

	public CardboardSwordItemRenderer(ItemModel inventoryModel, ItemModel heldModel) {
		this.inventoryModel = inventoryModel;
		this.heldModel = heldModel;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		ItemModel selected = displayContext == ItemDisplayContext.GUI ? inventoryModel : heldModel;
		if (selected != null)
			selected.update(state, stack, resolver, displayContext, level, owner, seed);
	}
}
