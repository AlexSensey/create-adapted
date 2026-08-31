package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.fluid.ResourceHandlerFluidAdapter.TransferCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class PortableFluidInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {

	protected IFluidHandler capability;
	private ResourceHandler<FluidResource> fluidResourceCapability;
	private boolean resourceTransaction;

	public PortableFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		capability = createEmptyHandler();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, AllBlockEntityTypes.PORTABLE_FLUID_INTERFACE.get(),
			(be, side) -> be.getFluidResourceCapability());
	}

	private ResourceHandler<FluidResource> getFluidResourceCapability() {
		if (fluidResourceCapability == null)
			fluidResourceCapability = new InterfaceFluidResourceHandler();
		return fluidResourceCapability;
	}

	@Override
	public void startTransferringTo(Contraption contraption, float distance) {
		capability = new InterfaceFluidHandler(contraption.getStorage().getFluids());
		invalidateCapability();
		if (level != null && !level.isClientSide())
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		super.startTransferringTo(contraption, distance);
	}

	@Override
	protected void invalidateCapability() {
		fluidResourceCapability = null;
		invalidateCapabilities();
	}

	@Override
	protected void stopTransferring() {
		capability = createEmptyHandler();
		invalidateCapability();
		if (level != null && !level.isClientSide())
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		super.stopTransferring();
	}

	private IFluidHandler createEmptyHandler() {
		return new InterfaceFluidHandler(new FluidTank(0));
	}

	public class InterfaceFluidHandler implements IFluidHandler {

		private IFluidHandler wrapped;

		public InterfaceFluidHandler(IFluidHandler wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int getTanks() {
			return wrapped.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return wrapped.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return wrapped.getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return wrapped.isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (!isConnected())
				return 0;
			int fill = wrapped.fill(resource, action);
			if (fill > 0 && action.execute() && !resourceTransaction)
				keepAlive();
			return fill;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (!canTransfer())
				return FluidStack.EMPTY;
			FluidStack drain = wrapped.drain(resource, action);
			if (!drain.isEmpty() && action.execute() && !resourceTransaction)
				keepAlive();
			return drain;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (!canTransfer())
				return FluidStack.EMPTY;
			FluidStack drain = wrapped.drain(maxDrain, action);
			if (!drain.isEmpty() && action.execute() && !resourceTransaction)
				keepAlive();
			return drain;
		}

		public void keepAlive() {
			onContentTransferred();
		}

	}

	private class InterfaceFluidResourceHandler implements ResourceHandler<FluidResource>, TransferCallback {
		private final FluidJournal journal = new FluidJournal();

		@Override
		public void onTransferCommitted() {
			onContentTransferred();
		}

		@Override
		public int size() {
			return capability.getTanks();
		}

		@Override
		public FluidResource getResource(int index) {
			return FluidResource.of(capability.getFluidInTank(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return capability.getFluidInTank(index)
				.getAmount();
		}

		@Override
		public long getCapacityAsLong(int index, FluidResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return capability.getTankCapacity(index);
		}

		@Override
		public boolean isValid(int index, FluidResource resource) {
			if (resource.isEmpty())
				return true;
			return capability.isFluidValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			FluidStack stack = resource.toStack(amount);
			int inserted = capability.fill(stack, FluidAction.SIMULATE);
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			resourceTransaction = true;
			try {
				return capability.fill(resource.toStack(inserted), FluidAction.EXECUTE);
			} finally {
				resourceTransaction = false;
			}
		}

		@Override
		public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			FluidStack extracted = capability.drain(resource.toStack(amount), FluidAction.SIMULATE);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			resourceTransaction = true;
			try {
				return capability.drain(extracted, FluidAction.EXECUTE)
					.getAmount();
			} finally {
				resourceTransaction = false;
			}
		}

		private NonNullList<FluidStack> createSnapshot() {
			NonNullList<FluidStack> snapshot = NonNullList.create();
			for (int i = 0; i < capability.getTanks(); i++)
				snapshot.add(capability.getFluidInTank(i)
					.copy());
			return snapshot;
		}

		private void restoreSnapshot(NonNullList<FluidStack> snapshot) {
			resourceTransaction = true;
			try {
				for (int i = 0; i < capability.getTanks(); i++)
					capability.drain(capability.getFluidInTank(i), FluidAction.EXECUTE);
				for (FluidStack stack : snapshot)
					if (!stack.isEmpty())
						capability.fill(stack.copy(), FluidAction.EXECUTE);
			} finally {
				resourceTransaction = false;
			}
		}

		private class FluidJournal extends SnapshotJournal<NonNullList<FluidStack>> {
			@Override
			protected NonNullList<FluidStack> createSnapshot() {
				return InterfaceFluidResourceHandler.this.createSnapshot();
			}

			@Override
			protected void revertToSnapshot(NonNullList<FluidStack> snapshot) {
				restoreSnapshot(snapshot);
			}

			@Override
			protected void onRootCommit(NonNullList<FluidStack> snapshot) {
				onContentTransferred();
			}
		}
	}

}
