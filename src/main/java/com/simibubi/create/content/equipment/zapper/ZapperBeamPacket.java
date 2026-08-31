package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.AllPackets;

import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public class ZapperBeamPacket extends ShootGadgetPacket {
	public static final StreamCodec<ByteBuf, ZapperBeamPacket> STREAM_CODEC = StreamCodec.composite(
			CatnipStreamCodecs.VEC3, packet -> packet.location,
			CatnipStreamCodecs.HAND, packet -> packet.hand,
			ByteBufCodecs.BOOL, packet -> packet.self,
			CatnipStreamCodecs.VEC3, packet -> packet.target,
			ZapperBeamPacket::new
	);

	private final Vec3 target;

	public ZapperBeamPacket(Vec3 start, InteractionHand hand, boolean self, Vec3 target) {
		super(start, hand, self);
		this.target = target;
	}

	public Vec3 target() {
		return target;
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.BEAM_EFFECT;
	}
}
