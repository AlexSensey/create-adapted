package com.simibubi.create.content.equipment.zapper;

import net.createmod.catnip.net.base.ClientboundPacketPayload;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public abstract class ShootGadgetPacket implements ClientboundPacketPayload {
	protected final Vec3 location;
	protected final InteractionHand hand;
	protected final boolean self;

	public ShootGadgetPacket(Vec3 location, InteractionHand hand, boolean self) {
		this.location = location;
		this.hand = hand;
		this.self = self;
	}

	@Override
	public void handle(Player player) {
		ShootableGadgetRenderHandler.handlePacket(this);
	}
}
