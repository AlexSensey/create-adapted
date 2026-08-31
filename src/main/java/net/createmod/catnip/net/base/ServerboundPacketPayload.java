package net.createmod.catnip.net.base;

import net.minecraft.server.level.ServerPlayer;

public interface ServerboundPacketPayload extends BasePacketPayload {
	void handle(ServerPlayer player);
}
