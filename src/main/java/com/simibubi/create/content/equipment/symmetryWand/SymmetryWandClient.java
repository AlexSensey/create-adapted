package com.simibubi.create.content.equipment.symmetryWand;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class SymmetryWandClient {
	public static void open(ItemStack wand, InteractionHand hand) {
		ScreenOpener.open(new SymmetryWandScreen(wand, hand));
	}
}
