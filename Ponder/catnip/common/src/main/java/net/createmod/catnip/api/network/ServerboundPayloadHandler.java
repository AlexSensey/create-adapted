package net.createmod.catnip.api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ServerboundPayloadHandler<T extends CustomPacketPayload> {
	void handle(T payload, ServerPlayer player);
}
