package com.simibubi.create.content.kinetics.steamEngine;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

public class SteamJetParticle extends SimpleAnimatedParticle {

	protected SteamJetParticle(ClientLevel world, SteamJetParticleData data, double x, double y, double z, double dx,
		double dy, double dz, SpriteSet sprite) {
		super(world, x, y, z, sprite, world.getRandom().nextFloat() * .5f);
		xd = yd = zd = 0;
		gravity = 0;
		quadSize = .375f;
		setLifetime(21);
		setPos(x, y, z);
		this.setSpriteFromAge(sprite);
	}

	public ParticleRenderType getRenderType() {
		return ParticleRenderType.SINGLE_QUADS;
	}

	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	public static class Factory implements ParticleProvider<SteamJetParticleData> {
		private final SpriteSet spriteSet;

		public Factory(SpriteSet animatedSprite) {
			this.spriteSet = animatedSprite;
		}

		@Override
		public Particle createParticle(SteamJetParticleData data, ClientLevel worldIn, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
			return new SteamJetParticle(worldIn, data, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}
}
