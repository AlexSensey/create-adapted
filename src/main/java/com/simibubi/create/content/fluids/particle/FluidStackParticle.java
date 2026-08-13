package com.simibubi.create.content.fluids.particle;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.fluids.potion.PotionFluid;

import net.createmod.catnip.api.client.platform.ClientFluidHelper;
import net.createmod.catnip.api.platform.services.ModFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidStackParticle extends SingleQuadParticle {
	private final float uo;
	private final float vo;
	private final FluidStack fluid;

	public static FluidStackParticle create(ParticleType<FluidParticleData> type, ClientLevel world, FluidStack fluid,
		double x, double y, double z, double vx, double vy, double vz) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return new BasinFluidParticle(world, fluid, x, y, z, vx, vy, vz);
		return new FluidStackParticle(world, fluid, x, y, z, vx, vy, vz);
	}

	public FluidStackParticle(ClientLevel world, FluidStack fluid, double x, double y, double z, double vx, double vy,
		double vz) {
		super(world, x, y, z, vx, vy, vz, null);
		FluidModel model = Minecraft.getInstance()
			.getModelManager()
			.getFluidStateModelSet()
			.get(fluid.getFluid()
				.defaultFluidState());
		this.setSprite(model.stillMaterial()
			.sprite());

		this.fluid = fluid;
		xd = vx;
		yd = vy;
		zd = vz;
		gravity = 1.0F;
		rCol = 0.8F;
		gCol = 0.8F;
		bCol = 0.8F;
		multiplyColor(ClientFluidHelper.INSTANCE.getColor((TypedInstance<Fluid>) fluid, null, null));
		quadSize /= 2.0F;
		uo = random.nextFloat() * 3.0F;
		vo = random.nextFloat() * 3.0F;
	}

	@Override
	protected int getLightCoords(float partialTicks) {
		int light = super.getLightCoords(partialTicks);
		int skyLight = light >> 20;
		int blockLight = (light >> 4) & 0xf;
		blockLight = Math.max(blockLight, ModFluidHelper.INSTANCE.getLuminosity((TypedInstance<Fluid>) fluid));
		return (skyLight << 20) | (blockLight << 4);
	}

	protected void multiplyColor(int color) {
		rCol *= (float) (color >> 16 & 255) / 255.0F;
		gCol *= (float) (color >> 8 & 255) / 255.0F;
		bCol *= (float) (color & 255) / 255.0F;
	}

	@Override
	protected float getU0() {
		return sprite.getU((uo + 1.0F) / 4.0F);
	}

	@Override
	protected float getU1() {
		return sprite.getU(uo / 4.0F);
	}

	@Override
	protected float getV0() {
		return sprite.getV(vo / 4.0F);
	}

	@Override
	protected float getV1() {
		return sprite.getV((vo + 1.0F) / 4.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (!canEvaporate())
			return;
		if (onGround)
			remove();
		if (!removed)
			return;
		if (!onGround && random.nextFloat() < 1 / 8f)
			return;

		int color = ClientFluidHelper.INSTANCE.getColor((TypedInstance<Fluid>) fluid, null, null);
		level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
			(float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F,
			(float) (color & 255) / 255.0F), x, y, z, 0, 0, 0);
	}

	protected boolean canEvaporate() {
		return fluid.getFluid() instanceof PotionFluid;
	}

	public @NotNull ParticleRenderType getRenderType() {
		return ParticleRenderType.SINGLE_QUADS;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT_TERRAIN;
	}

}
