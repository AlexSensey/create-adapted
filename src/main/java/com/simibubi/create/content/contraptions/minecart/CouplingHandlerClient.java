package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.AllItems;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CouplingHandlerClient {

	static AbstractMinecart selectedCart;
	private static final RandomSource RANDOM = RandomSource.create();

	public static void tick() {
		if (selectedCart == null)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || selectedCart.isRemoved() || selectedCart.level() != minecraft.level) {
			selectedCart = null;
			return;
		}

		spawnSelectionParticles(selectedCart.getBoundingBox(), false);
		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();
		if (!AllItems.MINECART_COUPLING.isIn(mainHand) && !AllItems.MINECART_COUPLING.isIn(offHand))
			selectedCart = null;
	}

	static void onCartClicked(Player player, AbstractMinecart entity) {
		if (Minecraft.getInstance().player != player)
			return;
		if (selectedCart == null || selectedCart == entity) {
			selectedCart = entity;
			spawnSelectionParticles(entity.getBoundingBox(), true);
			return;
		}

		spawnSelectionParticles(entity.getBoundingBox(), true);
		ClientNetworkHelper.INSTANCE.sendToServer(new CouplingCreationPacket(selectedCart, entity));
		selectedCart = null;
	}

	static void sneakClick() {
		selectedCart = null;
	}

	private static void spawnSelectionParticles(AABB bounds, boolean highlight) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;
		Vec3 center = bounds.getCenter();
		int amount = highlight ? 100 : 2;
		ParticleOptions particle = highlight ? ParticleTypes.END_ROD : new DustParticleOptions(0xffffff, 1);
		for (int i = 0; i < amount; i++) {
			Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, RANDOM, 1);
			double yOffset = offset.y;
			offset = offset.multiply(1, 0, 1)
				.normalize()
				.add(0, yOffset / 8f, 0)
				.add(center);
			level.addParticle(particle, offset.x, offset.y, offset.z, 0, 0, 0);
		}
	}
}
