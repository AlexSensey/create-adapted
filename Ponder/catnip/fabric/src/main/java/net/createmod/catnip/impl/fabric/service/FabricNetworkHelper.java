package net.createmod.catnip.impl.fabric.service;

import net.createmod.catnip.api.network.NetworkHelper;
import net.createmod.catnip.api.network.PayloadCodecRegistry;
import net.createmod.catnip.api.network.ServerboundPayloadHandler;
import net.createmod.catnip.api.platform.services.ModHooksHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class FabricNetworkHelper implements NetworkHelper {
	private static final PayloadCodecRegistry clientboundCodecs = PayloadTypeRegistry.clientboundPlay()::register;
	private static final PayloadCodecRegistry serverboundCodecs = PayloadTypeRegistry.serverboundPlay()::register;

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
		ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(payload, context.player()));
	}

	@Override
	public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	@Override
	public void sendToAllClients(CustomPacketPayload payload) {
		Packet<?> packet = ServerPlayNetworking.createClientboundPacket(payload);
		ModHooksHelper.INSTANCE.getServerOrThrow().getPlayerList().broadcastAll(packet);
	}

	@Override
	public void sendToClientsTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
		Packet<ClientCommonPacketListener> packet = ServerPlayNetworking.createClientboundPacket(payload);
		if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
			chunkCache.sendToTrackingPlayersAndSelf(entity, packet);
		} else {
			throw new IllegalStateException("Cannot send clientbound payloads on the client");
		}
	}

	@Override
	public void sendToClientsTrackingEntity(Entity entity, CustomPacketPayload payload) {
		Packet<ClientCommonPacketListener> packet = ServerPlayNetworking.createClientboundPacket(payload);
		if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
			chunkCache.sendToTrackingPlayers(entity, packet);
		} else {
			throw new IllegalStateException("Cannot send clientbound payloads on the client");
		}
	}

	@Override
	public void sendToClientsTrackingChunk(ServerLevel serverLevel, ChunkPos chunk, CustomPacketPayload payload) {
		for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(chunk, false)) {
			sendToClient(player, payload);
		}
	}

	@Override
	public void sendToClientsAround(ServerLevel serverLevel, Vec3 pos, double radius, CustomPacketPayload payload) {
		Packet<?> packet = ServerPlayNetworking.createClientboundPacket(payload);
		serverLevel.getServer().getPlayerList().broadcast(null, pos.x(), pos.y(), pos.z(), radius, serverLevel.dimension(), packet);
	}
}
