package net.createmod.catnip.api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/// Allows **serverbound** packet payloads to handle themselves when received, instead of separately registering a handler.
///
/// A clientbound version is not possible due to side unsafety.
public interface SelfHandlingPayload extends CustomPacketPayload {
	void handle(ServerPlayer player);
}
