package com.simibubi.create.content.contraptions.elevator;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ElevatorContactClient {
	public static void openScreen(Level level, BlockPos pos, Player player) {
		if (!(player instanceof LocalPlayer))
			return;
		if (!(level.getBlockEntity(pos) instanceof ElevatorContactBlockEntity be))
			return;

		ScreenOpener.open(new ElevatorContactScreen(be.getBlockPos(), be.shortName, be.longName, be.doorControls.mode));
	}
}
