package com.simibubi.create.content.equipment.clipboard;

import org.jetbrains.annotations.Nullable;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClipboardClient {

	public static void openScreen(Player player, DataComponentMap components, @Nullable BlockPos pos) {
		if (Minecraft.getInstance().player == player)
			ScreenOpener.open(new ClipboardScreen(player.getInventory().getSelectedSlot(), components, pos));
	}
}
