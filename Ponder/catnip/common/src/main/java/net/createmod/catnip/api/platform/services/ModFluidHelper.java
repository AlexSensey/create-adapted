package net.createmod.catnip.api.platform.services;

import net.createmod.catnip.api.platform.ServiceHelper;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/// Platform bridge for working with fluids.
///
/// **Important**: the methods in this class take a `TypedInstance<Fluid>`, but that isn't entirely correct.
/// - On Fabric, a `FluidVariant` is required.
/// - On Neoforge, a `FluidStack` is required.
///
/// TypedInstance<Fluid> is the best common supertype between platforms.
public interface ModFluidHelper {
	ModFluidHelper INSTANCE = ServiceHelper.load(ModFluidHelper.class);

	int getLuminosity(TypedInstance<Fluid> fluid);

	boolean isLighterThanAir(TypedInstance<Fluid> fluid);

	/// Create a new fluid instance based on the given [FluidState].
	///
	/// It's safe to cast the returned object to either `FluidVariant` or `FluidStack`, depending on the platform.
	TypedInstance<Fluid> instanceFor(FluidState state);
}
