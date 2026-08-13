package com.simibubi.create.api.contraption.storage.item.simple;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class ResourceHandlerItemAdapter implements IItemHandlerModifiable {
	private final ResourceHandler<ItemResource> handler;

	ResourceHandlerItemAdapter(ResourceHandler<ItemResource> handler) {
		this.handler = handler;
	}

	@Override
	public int getSlots() {
		return handler.size();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return ItemUtil.getStack(handler, slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate, null);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0)
			return ItemStack.EMPTY;
		ItemResource resource = handler.getResource(slot);
		if (resource.isEmpty())
			return ItemStack.EMPTY;
		try (Transaction transaction = Transaction.openRoot()) {
			int extracted = handler.extract(slot, resource, amount, transaction);
			if (!simulate)
				transaction.commit();
			return resource.toStack(extracted);
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return handler.getCapacityAsInt(slot, ItemResource.EMPTY);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return stack.isEmpty() || handler.isValid(slot, ItemResource.of(stack));
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		try (Transaction transaction = Transaction.openRoot()) {
			ItemResource current = handler.getResource(slot);
			if (!current.isEmpty())
				handler.extract(slot, current, handler.getAmountAsInt(slot), transaction);
			if (!stack.isEmpty()) {
				ItemResource replacement = ItemResource.of(stack);
				if (handler.insert(slot, replacement, stack.getCount(), transaction) != stack.getCount())
					return;
			}
			transaction.commit();
		}
	}
}
