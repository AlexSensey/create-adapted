package net.createmod.catnip.api.client.network;

import net.createmod.catnip.api.platform.ServiceHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ClientNetworkHelper {
	ClientNetworkHelper INSTANCE = ServiceHelper.load(ClientNetworkHelper.class);

	<T extends CustomPacketPayload> void registerPayloadHandler(CustomPacketPayload.Type<T> type, ClientboundPayloadHandler<T> handler);

	void sendToServer(CustomPacketPayload payload);
}
