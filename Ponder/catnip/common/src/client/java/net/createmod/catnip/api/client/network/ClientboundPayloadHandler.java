package net.createmod.catnip.api.client.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@FunctionalInterface
public interface ClientboundPayloadHandler<T extends CustomPacketPayload> {
	void handle(T payload, LocalPlayer player);
}
