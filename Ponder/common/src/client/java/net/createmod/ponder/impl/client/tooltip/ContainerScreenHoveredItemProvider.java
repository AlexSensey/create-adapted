package net.createmod.ponder.impl.client.tooltip;

import net.createmod.ponder.api.client.HoveredItemProvider;
import net.createmod.ponder.impl.client.mixin.AbstractContainerScreenAccessor;
import net.createmod.catnip.impl.client.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

enum ContainerScreenHoveredItemProvider implements HoveredItemProvider {
	INSTANCE;

	@Override
	public Item determineHoveredItem() {
		if (!(((MinecraftAccessor) Minecraft.getInstance()).catnip$getGui().screen() instanceof AbstractContainerScreenAccessor screen))
			return Items.AIR;

		Slot slot = screen.getHoveredSlot();
		return slot == null ? Items.AIR : slot.getItem().getItem();
	}

	public static void init() {
	    HoveredItemProvider.register(INSTANCE);
	}
}
