package com.simibubi.create.content.logistics.depot;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class DepotBlockEntity extends SmartBlockEntity implements Clearable {
	DepotBehaviour depotBehaviour;
	private ResourceHandler<ItemResource> itemResourceCapability;

	public DepotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.DEPOT.get(),
			(be, side) -> be.getItemResourceCapability());
	}

	protected ResourceHandler<ItemResource> getItemResourceCapability() {
		if (depotBehaviour == null)
			return null;
		if (itemResourceCapability == null)
			itemResourceCapability = new DepotResourceHandler();
		return itemResourceCapability;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(depotBehaviour = new DepotBehaviour(this));
		depotBehaviour.addSubBehaviours(behaviours);
	}

	@Override
	public void clearContent() {
		depotBehaviour.clearContent();
	}

	@Override
	public void invalidate() {
		itemResourceCapability = null;
		super.invalidate();
	}

	public ItemStack getHeldItem() {
		return depotBehaviour.getHeldItemStack();
	}

	public void setHeldItem(ItemStack item) {
		TransportedItemStack newStack = new TransportedItemStack(item);
		if (depotBehaviour.heldItem != null)
			newStack.angle = depotBehaviour.heldItem.angle;
		depotBehaviour.setHeldItem(newStack);
	}

	private class DepotResourceHandler implements ResourceHandler<ItemResource> {
		private final DepotJournal journal = new DepotJournal();

		@Override
		public int size() {
			return depotBehaviour.itemHandler.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(depotBehaviour.itemHandler.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return depotBehaviour.itemHandler.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return depotBehaviour.itemHandler.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return resource.isEmpty() || depotBehaviour.itemHandler.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack stack = resource.toStack(amount);
			ItemStack remainder = depotBehaviour.itemHandler.insertItem(index, stack, true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			depotBehaviour.itemHandler.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = depotBehaviour.itemHandler.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = depotBehaviour.itemHandler.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			depotBehaviour.itemHandler.extractItem(index, extracted.getCount(), false);
			return extracted.getCount();
		}

		private class DepotJournal extends SnapshotJournal<DepotSnapshot> {
			@Override
			protected DepotSnapshot createSnapshot() {
				List<TransportedItemStack> incoming = new ArrayList<>();
				for (TransportedItemStack stack : depotBehaviour.incoming)
					incoming.add(stack.copy());

				List<ItemStack> outputs = new ArrayList<>();
				for (int slot = 0; slot < depotBehaviour.processingOutputBuffer.getSlots(); slot++)
					outputs.add(depotBehaviour.processingOutputBuffer.getStackInSlot(slot)
						.copy());

				return new DepotSnapshot(depotBehaviour.heldItem == null ? null : depotBehaviour.heldItem.copy(),
					incoming, outputs);
			}

			@Override
			protected void revertToSnapshot(DepotSnapshot snapshot) {
				depotBehaviour.heldItem = snapshot.heldItem() == null ? null : snapshot.heldItem()
					.copy();
				depotBehaviour.incoming.clear();
				for (TransportedItemStack stack : snapshot.incoming())
					depotBehaviour.incoming.add(stack.copy());
				for (int slot = 0; slot < snapshot.outputs()
					.size(); slot++)
					depotBehaviour.processingOutputBuffer.setStackInSlot(slot, snapshot.outputs()
						.get(slot)
						.copy());
				notifyUpdate();
			}
		}
	}

	private record DepotSnapshot(TransportedItemStack heldItem, List<TransportedItemStack> incoming,
								 List<ItemStack> outputs) {}
}
