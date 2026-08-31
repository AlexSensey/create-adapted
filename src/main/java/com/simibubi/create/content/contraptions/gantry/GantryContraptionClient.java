package com.simibubi.create.content.contraptions.gantry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class GantryContraptionClient {

	private GantryContraptionClient() {}

	public static void handlePacket(GantryContraptionUpdatePacket packet) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || !(level.getEntity(packet.entityID()) instanceof GantryContraptionEntity entity))
			return;
		entity.applyClientUpdate(packet);
	}
}
