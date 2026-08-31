package com.simibubi.create.content.equipment.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class BacktankClient {
	private BacktankClient() {}

	public static Player getPlayer() {
		return Minecraft.getInstance().player;
	}
}
