package com.simibubi.create.foundation.networking;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class KineticBlockEntitySyncRequestPacket implements ServerboundPacketPayload {
	private static final int MAX_CHUNK_DISTANCE = 8;

	public static final StreamCodec<ByteBuf, KineticBlockEntitySyncRequestPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, packet -> packet.chunkX,
		ByteBufCodecs.VAR_INT, packet -> packet.chunkZ,
		KineticBlockEntitySyncRequestPacket::new
	);

	private final int chunkX;
	private final int chunkZ;

	public KineticBlockEntitySyncRequestPacket(int chunkX, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public KineticBlockEntitySyncRequestPacket(ChunkPos chunkPos) {
		this(chunkPos.x(), chunkPos.z());
	}

	@Override
	public void handle(ServerPlayer player) {
		if (player == null)
			return;
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;

		ChunkPos playerChunk = player.chunkPosition();
		if (Math.abs(playerChunk.x() - chunkX) > MAX_CHUNK_DISTANCE || Math.abs(playerChunk.z() - chunkZ) > MAX_CHUNK_DISTANCE)
			return;
		if (!serverLevel.hasChunk(chunkX, chunkZ))
			return;

		LevelChunk chunk = serverLevel.getChunk(chunkX, chunkZ);
		for (BlockEntity blockEntity : chunk.getBlockEntities()
			.values()) {
			if (!(blockEntity instanceof KineticBlockEntity))
				continue;
			if (!(blockEntity instanceof SyncedBlockEntity syncedBlockEntity))
				continue;

			BlockPos pos = blockEntity.getBlockPos();
			CompoundTag tag = syncedBlockEntity.writeClient(new CompoundTag(), serverLevel.registryAccess());
			CatnipServices.NETWORK.sendToClient(player, new BlockEntityUpdatePacket(pos, tag));
		}
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.KINETIC_BLOCK_ENTITY_SYNC_REQUEST;
	}
}
