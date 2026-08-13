package com.simibubi.create.content.processing.recipe;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;

public class ProcessingInventory extends ItemStackHandler {
	public float remainingTime;
	public float recipeDuration;
	public boolean appliedRecipe;
	public Consumer<ItemStack> callback;
	private boolean limit;

	public ProcessingInventory(Consumer<ItemStack> callback) {
		super(32);
		this.callback = callback;
	}

	public ProcessingInventory withSlotLimit(boolean limit) {
		this.limit = limit;
		return this;
	}

	@Override
	public int getSlotLimit(int slot) {
		return !limit ? super.getSlotLimit(slot) : 1;
	}

	public void clear() {
		for (int i = 0; i < getSlots(); i++)
			setStackInSlot(i, ItemStack.EMPTY);
		remainingTime = 0;
		recipeDuration = 0;
		appliedRecipe = false;
	}

	public boolean isEmpty() {
		for (int i = 0; i < getSlots(); i++)
			if (!getStackInSlot(i).isEmpty())
				return false;
		return true;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack insertItem = super.insertItem(slot, stack, simulate);
		if (slot == 0 && !(insertItem.getCount() == stack.getCount() && ItemStack.isSameItem(insertItem, stack)))
			callback.accept(getStackInSlot(slot));
		return insertItem;
	}

	public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider registries) {
		CompoundTag nbt = new CompoundTag();
		ListTag items = new ListTag();
		nbt.putInt("Size", getSlots());
		for (int slot = 0; slot < getSlots(); slot++) {
			ItemStack stack = getStackInSlot(slot);
			if (stack.isEmpty())
				continue;

			CompoundTag itemTag = new CompoundTag();
			itemTag.putInt("Slot", slot);
			ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
				.result()
				.ifPresent(stackTag -> itemTag.put("Stack", stackTag));
			if (itemTag.contains("Stack"))
				items.add(itemTag);
		}
		nbt.put("Items", items);
		nbt.putFloat("ProcessingTime", remainingTime);
		nbt.putFloat("RecipeTime", recipeDuration);
		nbt.putBoolean("AppliedRecipe", appliedRecipe);
		return nbt;
	}

	public void deserializeNBT(@NotNull HolderLookup.Provider registries, CompoundTag nbt) {
		remainingTime = nbt.getFloatOr("ProcessingTime", 0);
		recipeDuration = nbt.getFloatOr("RecipeTime", 0);
		appliedRecipe = nbt.getBooleanOr("AppliedRecipe", false);
		for (int slot = 0; slot < getSlots(); slot++)
			setStackInSlot(slot, ItemStack.EMPTY);
		ListTag items = nbt.getListOrEmpty("Items");
		for (int i = 0; i < items.size(); i++) {
			CompoundTag itemTag = items.getCompoundOrEmpty(i);
			int slot = itemTag.getIntOr("Slot", -1);
			if (slot < 0 || slot >= getSlots())
				continue;
			Tag stackTag = itemTag.get("Stack");
			if (stackTag == null)
				continue;
			ItemStack stack = ItemStack.OPTIONAL_CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE),
					stackTag)
				.result()
				.map(result -> result.getFirst())
				.orElse(ItemStack.EMPTY);
			setStackInSlot(slot, stack);
		}
		if (isEmpty())
			appliedRecipe = false;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return slot == 0 && isEmpty();
	}

}
