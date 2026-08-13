package com.simibubi.create.content.decoration.steamWhistle;

import java.util.Map;
import java.util.WeakHashMap;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only state for {@link WhistleBlockEntity}. Keeping the sound instance
 * here prevents client sound classes from appearing in the common block entity's
 * field descriptors on a dedicated server.
 */
final class WhistleBlockEntityClient {

	private static final Map<WhistleBlockEntity, WhistleSoundInstance> ACTIVE_SOUNDS = new WeakHashMap<>();

	private WhistleBlockEntityClient() {}

	static void tickAudio(WhistleBlockEntity blockEntity, WhistleSize size, boolean powered) {
		WhistleSoundInstance soundInstance = ACTIVE_SOUNDS.get(blockEntity);
		if (!powered) {
			if (soundInstance != null) {
				soundInstance.fadeOut();
				ACTIVE_SOUNDS.remove(blockEntity);
			}
			return;
		}

		float pitch = (float) Math.pow(2, -blockEntity.pitch / 12.0);
		boolean particle = blockEntity.getLevel()
			.getGameTime() % 8 == 0;
		Entity cameraEntity = Minecraft.getInstance()
			.getCameraEntity();
		Vec3 center = Vec3.atCenterOf(blockEntity.getBlockPos());
		Vec3 eyePosition = cameraEntity == null ? center : cameraEntity.getEyePosition();
		float maxVolume = (float) Mth.clamp((64 - eyePosition.distanceTo(center)) / 64, 0, 1);

		if (soundInstance == null || soundInstance.isStopped() || soundInstance.getOctave() != size) {
			soundInstance = new WhistleSoundInstance(size, blockEntity.getBlockPos());
			ACTIVE_SOUNDS.put(blockEntity, soundInstance);
			Minecraft.getInstance()
				.getSoundManager()
				.play(soundInstance);
			AllSoundEvents.WHISTLE_CHIFF.playAt(blockEntity.getLevel(), blockEntity.getBlockPos(), maxVolume * .175f,
				size == WhistleSize.SMALL ? pitch + .75f : pitch, false);
			particle = true;
		}

		soundInstance.keepAlive();
		soundInstance.setPitch(pitch);

		if (!particle)
			return;

		Direction facing = blockEntity.getBlockState()
			.getOptionalValue(WhistleBlock.FACING)
			.orElse(Direction.SOUTH);
		float angle = 180 + AngleHelper.horizontalAngle(facing);
		Vec3 sizeOffset = VecHelper.rotate(new Vec3(0, -0.4f, 1 / 16f * size.ordinal()), angle, Axis.Y);
		Vec3 offset = VecHelper.rotate(new Vec3(0, 1, 0.75f), angle, Axis.Y);
		Vec3 position = offset.scale(.45f)
			.add(sizeOffset)
			.add(center);
		Vec3 motion = offset.subtract(Vec3.atLowerCornerOf(facing.getUnitVec3i())
			.scale(.75f));
		blockEntity.getLevel()
			.addParticle(new SteamJetParticleData(1), position.x, position.y, position.z, motion.x, motion.y, motion.z);
	}
}
