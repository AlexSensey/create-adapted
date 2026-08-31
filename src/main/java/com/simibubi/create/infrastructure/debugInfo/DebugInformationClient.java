package com.simibubi.create.infrastructure.debugInfo;

import com.simibubi.create.infrastructure.debugInfo.element.DebugInfoSection;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class DebugInformationClient {
	private DebugInformationClient() {}

	public static void handle(ServerDebugInfoPacket packet, Player player) {
		StringBuilder output = new StringBuilder();
		List<DebugInfoSection> clientInfo = DebugInformation.getClientInfo();
		ServerDebugInfoPacket.printInfo("Client", player, clientInfo, output);
		output.append("\n\n").append(packet.serverInfo());
		Minecraft.getInstance().keyboardHandler.setClipboard(output.toString());
		player.sendSystemMessage(Component.translatable("command.debuginfo.saved_to_clipboard"));
	}
}
