package com.simibubi.create;

import java.lang.reflect.InvocationTargetException;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only packet handler wiring. The callback type contains LocalPlayer,
 * so this code must not live in {@link AllPackets}, which is initialized on
 * both physical sides.
 */
final class AllPacketsClient {

	private AllPacketsClient() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void registerHandlers() {
		for (AllPackets packet : AllPackets.values()) {
			if (!ClientboundPacketPayload.class.isAssignableFrom(packet.packetClass()))
				continue;
			ClientNetworkHelper.INSTANCE.registerPayloadHandler((CustomPacketPayload.Type) packet.getType(),
				(payload, player) -> {
					if (!(payload instanceof ClientboundPacketPayload))
						return;
					try {
						// The adapted Catnip runtime uses handle(LocalPlayer), while the
						// compatibility sources used to compile Create still expose
						// handle(Player). Invoke the concrete packet method directly so
						// the interface descriptor mismatch cannot break world login.
						payload.getClass()
							.getMethod("handle", Player.class)
							.invoke(payload, player);
					} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
						throw new IllegalStateException("Could not handle Create clientbound packet "
							+ payload.getClass().getName(), e);
					}
				});
		}
	}
}
