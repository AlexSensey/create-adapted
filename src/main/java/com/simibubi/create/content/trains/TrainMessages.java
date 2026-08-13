package com.simibubi.create.content.trains;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class TrainMessages {

	public static void actionBar(Player player, Component component) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(component));
			return;
		}
		if (player.level().isClientSide()) {
			clientActionBar(component);
			return;
		}
		player.sendSystemMessage(component);
	}

	private static void clientActionBar(Component component) {
		Minecraft.getInstance().gui.hud.setOverlayMessage(component, true);
	}

}
