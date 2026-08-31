package net.createmod.catnip.impl.fabric.client.service;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.network.ClientboundPayloadHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public final class FabricClientNetworkHelper implements ClientNetworkHelper {
	@Override
	public <T extends CustomPacketPayload> void registerPayloadHandler(Type<T> type, ClientboundPayloadHandler<T> handler) {
		ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(payload, context.player()));
	}

	@Override
	public void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}
}
