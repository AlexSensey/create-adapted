package com.simibubi.create.content.contraptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class ContraptionPacketClient {

	private ContraptionPacketClient() {}

	public static void handleStall(ContraptionStallPacket packet) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getEntity(packet.entityId()) instanceof AbstractContraptionEntity entity)
			entity.handleStallInformation(packet.x(), packet.y(), packet.z(), packet.angle());
	}

	public static void handleBlockChanged(ContraptionBlockChangedPacket packet) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getEntity(packet.entityId()) instanceof AbstractContraptionEntity entity)
			entity.handleBlockChange(packet.localPos(), packet.newState());
	}

	public static void handleDisassembly(ContraptionDisassemblyPacket packet) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getEntity(packet.entityId()) instanceof AbstractContraptionEntity entity)
			entity.moveCollidedEntitiesOnDisassembly(packet.transform());
	}

	public static void handleRelocation(ContraptionRelocationPacket packet) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getEntity(packet.entityId()) instanceof OrientedContraptionEntity entity)
			entity.nonDamageTicks = 10;
	}
}
