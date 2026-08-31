package net.createmod.catnip.api.network.registry;

import net.createmod.catnip.api.network.NetworkHelper;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import net.createmod.catnip.api.network.ServerboundPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

/// Utility for registering custom [packet payloads][CustomPacketPayload].
/// @param namespace the namespace to register payloads under
public record CatnipPayloadRegistrar(String namespace) {
	/// Register a clientbound payload. The handler must be registered separately.
	public <T extends CustomPacketPayload> Type<T> clientbound(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		Type<T> type = this.type(name);
		NetworkHelper.INSTANCE.clientboundCodecs().register(type, codec);
		return type;
	}

	/// Register a serverbound payload. The handler must be registered separately.
	public <T extends CustomPacketPayload> Type<T> serverbound(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		Type<T> type = this.type(name);
		NetworkHelper.INSTANCE.serverboundCodecs().register(type, codec);
		return type;
	}

	/// Register a serverbound payload, as well as a handler for it.
	public <T extends CustomPacketPayload> Type<T> serverbound(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerboundPayloadHandler<T> handler) {
		Type<T> type = this.serverbound(name, codec);
		NetworkHelper.INSTANCE.registerPayloadHandler(type, handler);
		return type;
	}

	/// Register a [self-handling][SelfHandlingPayload] serverbound payload.
	/// There's no need to separately register a handler.
	public <T extends SelfHandlingPayload> Type<T> selfHandlingServerbound(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		Type<T> type = this.serverbound(name, codec);
		NetworkHelper.INSTANCE.registerSelfHandlingPayload(type);
		return type;
	}

	private <T extends CustomPacketPayload> Type<T> type(String name) {
		return new Type<>(Identifier.fromNamespaceAndPath(this.namespace, name));
	}
}
