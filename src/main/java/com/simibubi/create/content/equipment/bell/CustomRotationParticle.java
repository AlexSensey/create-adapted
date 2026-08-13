package com.simibubi.create.content.equipment.bell;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

public class CustomRotationParticle extends SimpleAnimatedParticle {

	protected boolean mirror;
	protected int loopLength;

	public CustomRotationParticle(ClientLevel worldIn, double x, double y, double z, SpriteSet spriteSet, float yAccel) {
		super(worldIn, x, y, z, spriteSet, yAccel);
	}

	public void selectSpriteLoopingWithAge(SpriteSet sprite) {
		int loopFrame = age % loopLength;
		this.setSprite(sprite.get(loopFrame, loopLength));
	}

	public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
		Quaternionf quaternion = new Quaternionf(camera.rotation());
		if (roll != 0.0F) {
			float angle = Mth.lerp(partialTicks, oRoll, roll);
			quaternion.mul(Axis.ZP.rotation(angle));
		}
		return quaternion;
	}

}
