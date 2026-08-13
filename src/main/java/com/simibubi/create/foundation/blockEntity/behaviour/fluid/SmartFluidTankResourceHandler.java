package com.simibubi.create.foundation.blockEntity.behaviour.fluid;

import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class SmartFluidTankResourceHandler implements ResourceHandler<FluidResource> {

	private final SmartFluidTankBehaviour behaviour;

	public SmartFluidTankResourceHandler(SmartFluidTankBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	@Override
	public int size() {
		return behaviour.getTanks().length;
	}

	@Override
	public FluidResource getResource(int index) {
		return FluidResource.of(getTank(index).getFluid());
	}

	@Override
	public long getAmountAsLong(int index) {
		return getTank(index).getFluidAmount();
	}

	@Override
	public long getCapacityAsLong(int index, FluidResource resource) {
		if (!resource.isEmpty() && !isValid(index, resource))
			return 0;
		return getTank(index).getCapacity();
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		if (resource.isEmpty())
			return false;
		return getTank(index).isFluidValid(resource.toStack(1));
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		if (!behaviour.insertionAllowed || amount <= 0 || !isValid(index, resource))
			return 0;

		SmartFluidTank tank = getTank(index);
		FluidStack stack = resource.toStack(amount);
		int filled = tank.fillSilently(stack, FluidAction.SIMULATE);
		if (filled <= 0)
			return 0;

		new FluidJournal().updateSnapshots(transaction);
		return tank.fillSilently(resource.toStack(filled), FluidAction.EXECUTE);
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		if (!behaviour.extractionAllowed || amount <= 0 || resource.isEmpty())
			return 0;

		SmartFluidTank tank = getTank(index);
		FluidStack extracted = tank.drainSilently(resource.toStack(amount), FluidAction.SIMULATE);
		if (extracted.isEmpty())
			return 0;

		new FluidJournal().updateSnapshots(transaction);
		return tank.drainSilently(extracted, FluidAction.EXECUTE).getAmount();
	}

	private SmartFluidTank getTank(int index) {
		return behaviour.getTanks()[index].getTank();
	}

	private NonNullList<FluidStack> createSnapshot() {
		NonNullList<FluidStack> snapshot = NonNullList.create();
		for (SmartFluidTankBehaviour.TankSegment segment : behaviour.getTanks())
			snapshot.add(segment.getTank()
				.getFluid()
				.copy());
		return snapshot;
	}

	private class FluidJournal extends SnapshotJournal<NonNullList<FluidStack>> {
		@Override
		protected NonNullList<FluidStack> createSnapshot() {
			return SmartFluidTankResourceHandler.this.createSnapshot();
		}

		@Override
		protected void revertToSnapshot(NonNullList<FluidStack> snapshot) {
			for (int i = 0; i < snapshot.size(); i++)
				behaviour.getTanks()[i].getTank()
					.setFluidSilently(snapshot.get(i));
		}

		@Override
		protected void onRootCommit(NonNullList<FluidStack> snapshot) {
			for (SmartFluidTankBehaviour.TankSegment segment : behaviour.getTanks())
				segment.onFluidStackChanged();
		}
	}
}
