package net.createmod.catnip.impl.fabric.service;

import net.createmod.catnip.api.platform.services.ModFluidHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class FabricFluidHelper implements ModFluidHelper {
	@Override
	public int getLuminosity(TypedInstance<Fluid> fluid) {
		return FluidVariantAttributes.getLuminance((FluidVariant) fluid);
	}

	@Override
	public boolean isLighterThanAir(TypedInstance<Fluid> fluid) {
		return FluidVariantAttributes.isLighterThanAir((FluidVariant) fluid);
	}

	@Override
	public FluidVariant instanceFor(FluidState state) {
		return FluidVariant.of(state.getType());
	}
}
