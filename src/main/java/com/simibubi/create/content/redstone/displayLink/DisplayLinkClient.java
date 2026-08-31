package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.api.behaviour.display.DisplayTarget;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class DisplayLinkClient {
	public static AABB getSelectionBounds(BlockPos pos, AABB fallback) {
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return fallback;
		DisplayTarget target = DisplayTarget.get(level, pos);
		return target == null ? fallback : target.getMultiblockBounds(level, pos);
	}
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
