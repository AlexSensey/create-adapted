package net.createmod.catnip.impl.neoforge.service;

import net.createmod.catnip.api.platform.services.ModFluidHelper;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.fluids.FluidStack;

public class NeoForgeFluidHelper implements ModFluidHelper {
	@Override
	public int getLuminosity(TypedInstance<Fluid> fluid) {
		return ((FluidStack) fluid).getFluidType().getLightLevel();
	}

	@Override
	public boolean isLighterThanAir(TypedInstance<Fluid> fluid) {
		return ((FluidStack) fluid).getFluidType().isLighterThanAir();
	}

	@Override
	public FluidStack instanceFor(FluidState state) {
		return new FluidStack(state.getType(), 1000);
	}
}
