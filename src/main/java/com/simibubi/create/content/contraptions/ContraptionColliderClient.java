package com.simibubi.create.content.contraptions;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest;
import com.simibubi.create.content.contraptions.sync.ClientMotionPacket;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only pieces of contraption collision handling.
 *
 * <p>Keep every reference to {@code net.minecraft.client} types in this class. The common
 * collider is loaded by dedicated servers and must remain safe to link there.</p>
 */
final class ContraptionColliderClient {

	private static final MutablePair<WeakReference<AbstractContraptionEntity>, Double> safetyLock =
		new MutablePair<>();
	private static final Map<AbstractContraptionEntity, Map<Player, Double>> remoteSafetyLocks =
		new WeakHashMap<>();
	private static int packetCooldown;

	private ContraptionColliderClient() {}

	static boolean isClientPlayerEntity(Entity entity) {
		return entity instanceof LocalPlayer;
	}

	static void sendClientMotion(Vec3 motion, float limbSwing) {
		ClientNetworkHelper.INSTANCE.sendToServer(new ClientMotionPacket(motion, true, limbSwing));
	}

	static void sendTrainCollision(int damage, int contraptionId) {
		ClientNetworkHelper.INSTANCE.sendToServer(new TrainCollisionPacket(damage, contraptionId));
	}

	static void setSafetyLock(AbstractContraptionEntity contraptionEntity, double offset) {
		safetyLock.setLeft(new WeakReference<>(contraptionEntity));
		safetyLock.setRight(offset);
	}

	static void saveClientPlayerFromClippingIfNeeded(AbstractContraptionEntity contraptionEntity,
		Vec3 contraptionMotion) {
		if (safetyLock.left == null || safetyLock.left.get() != contraptionEntity)
			return;

		LocalPlayer entity = Minecraft.getInstance().player;
		if (entity == null || entity.isPassenger())
			return;

		double prevDiff = safetyLock.right;
		double currentDiff = entity.getY() - contraptionEntity.getY();
		double motion = contraptionMotion.subtract(entity.getDeltaMovement()).y;
		double trend = Math.signum(currentDiff - prevDiff);

		ClientPacketListener handler = entity.connection;
		if (handler.getOnlinePlayers()
			.size() > 1) {
			if (packetCooldown > 0)
				packetCooldown--;
			if (packetCooldown == 0) {
				ClientNetworkHelper.INSTANCE.sendToServer(
					new ContraptionColliderLockPacketRequest(contraptionEntity.getId(), currentDiff));
				packetCooldown = 3;
			}
		}

		if (trend == 0)
			return;
		if (trend == Math.signum(motion))
			return;

		double speed = contraptionMotion.multiply(0, 1, 0)
			.lengthSqr();
		if (trend > 0 && speed < 0.1)
			return;
		if (speed < 0.05)
			return;

		if (!savePlayerFromClipping(entity, contraptionEntity, prevDiff))
			safetyLock.setLeft(null);
	}

	static void lockPacketReceived(int contraptionId, int remotePlayerId, double suggestedOffset) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;
		if (!(level.getEntity(contraptionId) instanceof ControlledContraptionEntity contraptionEntity))
			return;
		if (!(level.getEntity(remotePlayerId) instanceof RemotePlayer player))
			return;
		remoteSafetyLocks.computeIfAbsent(contraptionEntity, $ -> new WeakHashMap<>())
			.put(player, suggestedOffset);
	}

	static void saveRemotePlayerFromClipping(Player entity, AbstractContraptionEntity contraptionEntity,
		Vec3 contraptionMotion) {
		if (entity.isPassenger())
			return;

		Map<Player, Double> locksOnThisContraption =
			remoteSafetyLocks.getOrDefault(contraptionEntity, Collections.emptyMap());
		double prevDiff = locksOnThisContraption.getOrDefault(entity, entity.getY() - contraptionEntity.getY());
		if (!savePlayerFromClipping(entity, contraptionEntity, prevDiff))
			locksOnThisContraption.remove(entity);
	}

	private static boolean savePlayerFromClipping(Player entity, AbstractContraptionEntity contraptionEntity,
		double yStartOffset) {
		AABB bb = entity.getBoundingBox()
			.deflate(1 / 4f, 0, 1 / 4f);
		double shortestDistance = Double.MAX_VALUE;
		double yStart = entity.maxUpStep() + contraptionEntity.getY() + yStartOffset;
		double rayLength = Math.max(5, Math.abs(entity.getY() - yStart));

		for (int rayIndex = 0; rayIndex < 4; rayIndex++) {
			Vec3 start = new Vec3(rayIndex / 2 == 0 ? bb.minX : bb.maxX, yStart,
				rayIndex % 2 == 0 ? bb.minZ : bb.maxZ);
			Vec3 end = start.add(0, -rayLength, 0);

			BlockHitResult hitResult = ContraptionHandlerClient.rayTraceContraption(start, end, contraptionEntity);
			if (hitResult == null)
				continue;

			Vec3 hit = contraptionEntity.toGlobalVector(hitResult.getLocation(), 1);
			double hitDiff = start.y - hit.y;
			if (shortestDistance > hitDiff)
				shortestDistance = hitDiff;
		}

		if (shortestDistance > rayLength)
			return false;
		entity.setPos(entity.getX(), yStart - shortestDistance, entity.getZ());
		return true;
	}
}
