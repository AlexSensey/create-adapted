package com.simibubi.create.foundation.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ResourceHandlerFluidAdapter implements IFluidHandler {
	private final ResourceHandler<FluidResource> handler;

	public interface TransferCallback {
		void onTransferCommitted();
	}

	public ResourceHandlerFluidAdapter(ResourceHandler<FluidResource> handler) {
		this.handler = handler;
	}

	@Override
	public int getTanks() {
		return handler.size();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return FluidUtil.getStack(handler, tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		return handler.getCapacityAsInt(tank, FluidResource.EMPTY);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return handler.isValid(tank, FluidResource.of(stack));
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty())
			return 0;
		try (Transaction transaction = Transaction.openRoot()) {
			int inserted = handler.insert(FluidResource.of(resource), resource.getAmount(), transaction);
			if (action.execute()) {
				transaction.commit();
				if (inserted > 0)
					notifyTransferCommitted();
			}
			return inserted;
		}
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty())
			return FluidStack.EMPTY;
		try (Transaction transaction = Transaction.openRoot()) {
			int extracted = handler.extract(FluidResource.of(resource), resource.getAmount(), transaction);
			if (action.execute()) {
				transaction.commit();
				if (extracted > 0)
					notifyTransferCommitted();
			}
			return extracted == 0 ? FluidStack.EMPTY : resource.copyWithAmount(extracted);
		}
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0)
			return FluidStack.EMPTY;
		try (Transaction transaction = Transaction.openRoot()) {
			var extracted = ResourceHandlerUtil.extractFirst(handler, $ -> true, maxDrain, transaction);
			if (action.execute()) {
				transaction.commit();
				if (extracted != null && extracted.amount() > 0)
					notifyTransferCommitted();
			}
			return extracted == null ? FluidStack.EMPTY : extracted.resource().toStack(extracted.amount());
		}
	}

	private void notifyTransferCommitted() {
		if (handler instanceof TransferCallback callback)
			callback.onTransferCommitted();
	}
}
