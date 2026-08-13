package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllPackets;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.track.TrackPlacement.ConnectingFrom;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record TrackSelectionPacket(boolean mainHand, ConnectingFrom selection) implements ServerboundPacketPayload {
	public static final StreamCodec<ByteBuf, TrackSelectionPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, TrackSelectionPacket::mainHand,
		ConnectingFrom.STREAM_CODEC, TrackSelectionPacket::selection,
		TrackSelectionPacket::new
	);

	@Override
	public void handle(ServerPlayer sender) {
		ItemStack stack = sender.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
		if (!AllTags.AllBlockTags.TRACKS.matches(stack))
			return;
		if (!AllTags.AllBlockTags.TRACKS.matches(sender.level()
			.getBlockState(selection.pos())))
			return;
		stack.set(AllDataComponents.TRACK_CONNECTING_FROM, selection);
		TrackBlockItem.rememberSelection(sender, stack);
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.SELECT_TRACK;
	}
}
