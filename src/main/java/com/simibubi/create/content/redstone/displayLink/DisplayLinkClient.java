package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class DisplayLinkClient {
	public static void openScreen(Level level, BlockPos pos, Player player) {
		if (!(player instanceof LocalPlayer))
			return;
		if (!(level.getBlockEntity(pos) instanceof DisplayLinkBlockEntity be))
			return;

		if (be.targetOffset.equals(BlockPos.ZERO)) {
			player.sendOverlayMessage(CreateLang.translateDirect("display_link.invalid"));
			return;
		}
		ScreenOpener.open(new DisplayLinkScreen(be));
	}
}
