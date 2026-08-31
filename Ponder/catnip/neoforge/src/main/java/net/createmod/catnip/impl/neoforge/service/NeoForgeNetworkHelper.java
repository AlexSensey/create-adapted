package net.createmod.catnip.impl.neoforge.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.createmod.catnip.api.network.NetworkHelper;
import net.createmod.catnip.api.network.PayloadCodecRegistry;
import net.createmod.catnip.api.network.ServerboundPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeNetworkHelper implements NetworkHelper {
	private static class NeoForgePayloadCodecRegistry implements PayloadCodecRegistry {
		private static final Set<String> registeredNamespaces = Collections.synchronizedSet(new HashSet<>());
		private final Map<Type<?>, StreamCodec<? super RegistryFriendlyByteBuf, ?>> payloads = Collections.synchronizedMap(new HashMap<>());

		@Override
		public <T extends CustomPacketPayload> void register(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
			this.payloads.put(type, codec);
			if (registeredNamespaces.add(type.id().getNamespace())) {
				ModContainer container = ModList.get().getModContainerById(type.id().getNamespace()).orElseThrow();
				container.getEventBus().addListener((RegisterPayloadHandlersEvent e) -> {
					registerPackets(e, type.id().getNamespace());
				});
			}
		}
	}

	private static final NeoForgePayloadCodecRegistry clientboundCodecs = new NeoForgePayloadCodecRegistry();
	private static final NeoForgePayloadCodecRegistry serverboundCodecs = new NeoForgePayloadCodecRegistry();
	private static final Map<Type<?>, ServerboundPayloadHandler<?>> handlers = Collections.synchronizedMap(new HashMap<>());

	@Override
	public PayloadCodecRegistry clientboundCodecs() {
		return clientboundCodecs;
	}

	@Override
	public PayloadCodecRegistry serverboundCodecs() {
		return serverboundCodecs;
	}

	@Override
	public <T extends CustomPacketPayload> void registerPayloadHandler(Type<T> type, ServerboundPayloadHandler<T> handler) {
		handlers.put(type, handler);
	}

	private static void registerPackets(RegisterPayloadHandlersEvent e, String namespace) {
		PayloadRegistrar registrar = e.registrar("1");

		clientboundCodecs.payloads.forEach((type, streamCodec) -> {
			if (type.id().getNamespace().equals(namespace)) {
				registrar.playToClient((Type) type, (StreamCodec) streamCodec);
			}
		});

		serverboundCodecs.payloads.forEach((type, streamCodec) -> {
			if (type.id().getNamespace().equals(namespace)) {
				registrar.playToServer((Type) type, (StreamCodec) streamCodec, (payload, context) -> {
					context.enqueueWork(() -> {
						ServerboundPayloadHandler handler = handlers.get(type);
						handler.handle(payload, (ServerPlayer) context.player());
					});
				});
			}
		});
	}

	@Override
	public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	@Override
	public void sendToAllClients(CustomPacketPayload payload) {
		PacketDistributor.sendToAllPlayers(payload);
	}

	@Override
	public void sendToClientsTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
	}

	@Override
	public void sendToClientsTrackingEntity(Entity entity, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
	}

	@Override
	public void sendToClientsTrackingChunk(ServerLevel serverLevel, ChunkPos chunk, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk, payload);
	}

	@Override
	public void sendToClientsAround(ServerLevel serverLevel, Vec3 pos, double radius, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayersNear(serverLevel, null, pos.x(), pos.y(), pos.z(), radius, payload);
	}
}
