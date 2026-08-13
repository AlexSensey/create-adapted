package com.simibubi.create.content.trains;

import org.joml.Quaternionf;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class CubeParticle extends SingleQuadParticle {

	private final float cubeScale;
	private boolean hot;
	private boolean billowing;

	protected CubeParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY,
		double motionZ, float scale, SpriteSet sprites) {
		super(world, x, y, z, motionX, motionY, motionZ, sprites.first());
		this.xd = motionX;
		this.yd = motionY;
		this.zd = motionZ;
		this.cubeScale = scale;
		this.quadSize = scale;
	}

	public void setScale(float scale) {
		this.setSize(scale * .5f, scale * .5f);
	}

	public void averageAge(int age) {
		this.lifetime = (int) (age + (random.nextDouble() * 2 - 1) * 8);
	}

	public void setHot(boolean hot) {
		this.hot = hot;
		this.speedUpWhenYMotionIsBlocked = hot;
	}

	@Override
	public void tick() {
		if (hot && age > 0) {
			if (yo == y) {
				billowing = true;
				if (xd == 0 && zd == 0) {
					Vec3 diff = Vec3.atLowerCornerOf(BlockPos.containing(x, y, z))
						.add(.5, .5, .5)
						.subtract(x, y, z);
					xd = -diff.x * .1;
					zd = -diff.z * .1;
				}
				xd *= 1.1;
				yd *= .9;
				zd *= 1.1;
			} else if (billowing) {
				yd *= 1.2;
			}
		}
		super.tick();
	}

	@Override
	public float getQuadSize(float partialTick) {
		float progress = lifetime == 0 ? 1 : Mth.clamp((age + partialTick) / lifetime, 0, 1);
		return cubeScale * (1 - progress * progress * progress);
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		// Minecraft 26.2 batches particles as render-state quads. Submit the six
		// fixed faces separately to retain Create's original shrinking cube.
		extractRotatedQuad(state, camera, new Quaternionf(), partialTick);
		extractRotatedQuad(state, camera, new Quaternionf().rotateY((float) Math.PI), partialTick);
		extractRotatedQuad(state, camera, new Quaternionf().rotateY((float) Math.PI / 2), partialTick);
		extractRotatedQuad(state, camera, new Quaternionf().rotateY((float) -Math.PI / 2), partialTick);
		extractRotatedQuad(state, camera, new Quaternionf().rotateX((float) Math.PI / 2), partialTick);
		extractRotatedQuad(state, camera, new Quaternionf().rotateX((float) -Math.PI / 2), partialTick);
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public static class Factory implements ParticleProvider<CubeParticleData> {
		private final SpriteSet sprites;

		public Factory(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(CubeParticleData data, ClientLevel world, double x, double y, double z,
			double motionX, double motionY, double motionZ, RandomSource random) {
			CubeParticle particle = new CubeParticle(world, x, y, z, motionX, motionY, motionZ, data.scale, sprites);
			particle.setColor(data.r, data.g, data.b);
			particle.setScale(data.scale);
			particle.averageAge(data.avgAge);
			particle.setHot(data.hot);
			return particle;
		}
	}
}
