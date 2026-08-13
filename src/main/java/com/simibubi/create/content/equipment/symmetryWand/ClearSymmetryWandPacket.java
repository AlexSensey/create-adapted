package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record ClearSymmetryWandPacket(InteractionHand hand) implements ServerboundPacketPayload {
	public static final StreamCodec<ByteBuf, ClearSymmetryWandPacket> STREAM_CODEC =
		CatnipStreamCodecs.HAND.map(ClearSymmetryWandPacket::new, ClearSymmetryWandPacket::hand);

	@Override
	public void handle(ServerPlayer player) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SymmetryWandItem)
			stack.set(AllDataComponents.SYMMETRY_WAND_ENABLE, false);
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.CLEAR_SYMMETRY_WAND;
	}
}
