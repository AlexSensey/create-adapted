package com.simibubi.create.content.contraptions.actors.psi;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class PortableItemInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {

	protected IItemHandlerModifiable capability;
	private ResourceHandler<ItemResource> itemResourceCapability;
	private boolean resourceTransaction;

	public PortableItemInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		capability = createEmptyHandler();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.PORTABLE_STORAGE_INTERFACE.get(),
			(be, side) -> be.getItemResourceCapability());
	}

	private ResourceHandler<ItemResource> getItemResourceCapability() {
		if (itemResourceCapability == null)
			itemResourceCapability = new InterfaceItemResourceHandler();
		return itemResourceCapability;
	}

	@Override
	public void startTransferringTo(Contraption contraption, float distance) {
		capability = new InterfaceItemHandler(contraption.getStorage().getAllItems());
		invalidateCapability();
		if (level != null && !level.isClientSide())
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		super.startTransferringTo(contraption, distance);
	}

	@Override
	protected void stopTransferring() {
		capability = createEmptyHandler();
		invalidateCapability();
		if (level != null && !level.isClientSide())
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		super.stopTransferring();
	}

	private IItemHandlerModifiable createEmptyHandler() {
		return new InterfaceItemHandler(new ItemStackHandler(0));
	}

	@Override
	protected void invalidateCapability() {
		itemResourceCapability = null;
		invalidateCapabilities();
	}

	class InterfaceItemHandler extends ItemHandlerWrapper {

		public InterfaceItemHandler(IItemHandlerModifiable wrapped) {
			super(wrapped);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (!canTransfer())
				return ItemStack.EMPTY;
			ItemStack extractItem = super.extractItem(slot, amount, simulate);
			if (!simulate && !extractItem.isEmpty() && !resourceTransaction)
				onContentTransferred();
			return extractItem;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!canTransfer())
				return stack;
			ItemStack insertItem = super.insertItem(slot, stack, simulate);
			if (!simulate && !ItemStack.matches(insertItem, stack) && !resourceTransaction)
				onContentTransferred();
			return insertItem;
		}

	}

	private class InterfaceItemResourceHandler implements ResourceHandler<ItemResource> {
		private final ItemJournal journal = new ItemJournal();

		@Override
		public int size() {
			return capability.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(capability.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return capability.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return capability.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return resource.isEmpty() || capability.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack stack = resource.toStack(amount);
			ItemStack remainder = capability.insertItem(index, stack, true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			resourceTransaction = true;
			try {
				capability.insertItem(index, resource.toStack(inserted), false);
			} finally {
				resourceTransaction = false;
			}
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = capability.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = capability.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			resourceTransaction = true;
			try {
				capability.extractItem(index, extracted.getCount(), false);
			} finally {
				resourceTransaction = false;
			}
			return extracted.getCount();
		}

		private List<ItemStack> createSnapshot() {
			List<ItemStack> snapshot = new ArrayList<>();
			for (int slot = 0; slot < capability.getSlots(); slot++)
				snapshot.add(capability.getStackInSlot(slot)
					.copy());
			return snapshot;
		}

		private void restoreSnapshot(List<ItemStack> snapshot) {
			for (int slot = 0; slot < snapshot.size(); slot++)
				capability.setStackInSlot(slot, snapshot.get(slot)
					.copy());
		}

		private class ItemJournal extends SnapshotJournal<List<ItemStack>> {
			@Override
			protected List<ItemStack> createSnapshot() {
				return InterfaceItemResourceHandler.this.createSnapshot();
			}

			@Override
			protected void revertToSnapshot(List<ItemStack> snapshot) {
				restoreSnapshot(snapshot);
			}

			@Override
			protected void onRootCommit(List<ItemStack> snapshot) {
				onContentTransferred();
			}
		}
	}

}
