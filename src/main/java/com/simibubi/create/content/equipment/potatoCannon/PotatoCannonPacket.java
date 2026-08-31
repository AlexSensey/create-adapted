package com.simibubi.create.content.equipment.potatoCannon;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.equipment.zapper.ShootGadgetPacket;

import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PotatoCannonPacket extends ShootGadgetPacket {
	public static final StreamCodec<RegistryFriendlyByteBuf, PotatoCannonPacket> STREAM_CODEC = StreamCodec.composite(
		CatnipStreamCodecs.VEC3, packet -> packet.location,
		CatnipStreamCodecs.VEC3, packet -> packet.motion,
		ItemStack.OPTIONAL_STREAM_CODEC, packet -> packet.item,
		CatnipStreamCodecs.HAND, packet -> packet.hand,
		ByteBufCodecs.FLOAT, packet -> packet.pitch,
		ByteBufCodecs.BOOL, packet -> packet.self,
		PotatoCannonPacket::new
	);

	private final float pitch;
	private final Vec3 motion;
	private final ItemStack item;

	public PotatoCannonPacket(Vec3 location, Vec3 motion, ItemStack item, InteractionHand hand, float pitch, boolean self) {
		super(location, hand, self);
		this.motion = motion;
		this.item = item;
		this.pitch = pitch;
	}

	public float pitch() {
		return pitch;
	}

	public Vec3 motion() {
		return motion;
	}

	public ItemStack item() {
		return item;
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.POTATO_CANNON;
	}
}
