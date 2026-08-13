package com.simibubi.create.content.redstone.thresholdSwitch;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ThresholdSwitchClient {
	public static void openScreen(Level level, BlockPos pos, Player player) {
		if (!(player instanceof LocalPlayer))
			return;
		if (!(level.getBlockEntity(pos) instanceof ThresholdSwitchBlockEntity be))
			return;

		ScreenOpener.open(new ThresholdSwitchScreen(be));
	}
}
