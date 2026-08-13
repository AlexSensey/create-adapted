package com.simibubi.create.foundation.networking;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlockEntityUpdatePacket implements ClientboundPacketPayload {
	public static final StreamCodec<ByteBuf, BlockEntityUpdatePacket> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, packet -> packet.pos,
		ByteBufCodecs.COMPOUND_TAG, packet -> packet.tag,
		BlockEntityUpdatePacket::new
	);

	private final BlockPos pos;
	private final CompoundTag tag;

	public BlockEntityUpdatePacket(BlockPos pos, CompoundTag tag) {
		this.pos = pos;
		this.tag = tag;
	}

	@Override
	public void handle(Player player) {
		BlockEntity blockEntity = player.level()
			.getBlockEntity(pos);
		if (blockEntity instanceof SyncedBlockEntity syncedBlockEntity)
			syncedBlockEntity.readClient(tag, player.level()
				.registryAccess());
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.BLOCK_ENTITY_UPDATE;
	}
}
