package com.simibubi.create.content.kinetics.transmission.sequencer;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SequencedGearshiftClient {
	public static void openScreen(Level level, BlockPos pos, Player player) {
		if (!(player instanceof LocalPlayer))
			return;
		if (!(level.getBlockEntity(pos) instanceof SequencedGearshiftBlockEntity be))
			return;

		ScreenOpener.open(new SequencedGearshiftScreen(be));
	}
}
