package com.simibubi.create.foundation.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;

public class LegacyItemHandlerSerialization {
	public static ItemStack readItemStack(HolderLookup.Provider registries, CompoundTag tag) {
		return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
			.result()
			.orElse(ItemStack.EMPTY);
	}

	public static ItemStackHandler readItemStackHandler(HolderLookup.Provider registries, CompoundTag tag,
		int fallbackSize) {
		int size = Math.max(0, tag.getIntOr("Size", fallbackSize));
		ItemStackHandler handler = new ItemStackHandler(size);
		for (Tag itemTag : tag.getListOrEmpty("Items")) {
			if (!(itemTag instanceof CompoundTag itemCompound))
				continue;
			int slot = Byte.toUnsignedInt(itemCompound.getByteOr("Slot", (byte) -1));
			if (slot >= size)
				continue;
			ItemStack stack = readItemStack(registries, itemCompound);
			if (!stack.isEmpty())
				handler.setStackInSlot(slot, stack);
		}
		return handler;
	}
}
