package com.simibubi.create.foundation.networking;

import com.simibubi.create.Create;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Required handshake marker that rejects mismatched Create: Adapted builds. */
public enum CreateExactVersionPayload implements CustomPacketPayload {
	INSTANCE;

	public static final Type<CreateExactVersionPayload> TYPE =
		new Type<>(Create.asResource("exact_version"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CreateExactVersionPayload> STREAM_CODEC =
		StreamCodec.unit(INSTANCE);

	public static void register(IEventBus modEventBus, String version) {
		modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar(version)
			.playBidirectional(TYPE, STREAM_CODEC, CreateExactVersionPayload::handle,
				CreateExactVersionPayload::handle));
	}

	private static void handle(CreateExactVersionPayload payload, IPayloadContext context) {
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
