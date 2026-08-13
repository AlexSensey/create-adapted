package com.simibubi.create.foundation.fluid;

import java.util.function.Consumer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class SmartFluidTank extends FluidTank {

	private Consumer<FluidStack> updateCallback;
	private boolean suppressUpdateCallback;

	public SmartFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
		super(capacity);
		this.updateCallback = updateCallback;
	}

	@Override
	protected void onContentsChanged() {
		super.onContentsChanged();
		if (suppressUpdateCallback)
			return;
		updateCallback.accept(getFluid());
	}

	@Override
	public void setFluid(FluidStack stack) {
		super.setFluid(stack);
		if (suppressUpdateCallback)
			return;
		updateCallback.accept(stack);
	}

	public void setFluidSilently(FluidStack stack) {
		suppressUpdateCallback = true;
		try {
			super.setFluid(stack);
		} finally {
			suppressUpdateCallback = false;
		}
	}

	public int fillSilently(FluidStack stack, FluidAction action) {
		suppressUpdateCallback = true;
		try {
			return super.fill(stack, action);
		} finally {
			suppressUpdateCallback = false;
		}
	}

	public FluidStack drainSilently(FluidStack stack, FluidAction action) {
		suppressUpdateCallback = true;
		try {
			return super.drain(stack, action);
		} finally {
			suppressUpdateCallback = false;
		}
	}

}
