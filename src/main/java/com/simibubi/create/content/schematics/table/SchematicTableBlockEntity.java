package com.simibubi.create.content.schematics.table;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.LegacyItemHandlerSerialization;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.ItemStackHandler;

public class SchematicTableBlockEntity extends SmartBlockEntity implements MenuProvider, Clearable {
	public SchematicTableInventory inventory;
	public boolean isUploading;
	public String uploadingSchematic;
	public float uploadingProgress;
	public boolean sendUpdate;

	public class SchematicTableInventory extends ItemStackHandler {
		public SchematicTableInventory() {
			super(2);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			setChanged();
		}
	}

	public SchematicTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new SchematicTableInventory();
		uploadingSchematic = null;
		uploadingProgress = 0;
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		readInventory(compound.getCompoundOrEmpty("Inventory"), registries);
		super.read(compound, registries, clientPacket);
		if (!clientPacket)
			return;
		if (compound.contains("Uploading")) {
			isUploading = true;
			uploadingSchematic = compound.getStringOr("Schematic", "");
			uploadingProgress = compound.getFloatOr("Progress", 0);
		} else {
			isUploading = false;
			uploadingSchematic = null;
			uploadingProgress = 0;
		}
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("Inventory", writeInventory(registries));
		super.write(compound, registries, clientPacket);
		if (clientPacket && isUploading) {
			compound.putBoolean("Uploading", true);
			compound.putString("Schematic", uploadingSchematic);
			compound.putFloat("Progress", uploadingProgress);
		}
	}

	private void readInventory(CompoundTag tag, HolderLookup.Provider registries) {
		ItemStackHandler loaded =
			LegacyItemHandlerSerialization.readItemStackHandler(registries, tag, inventory.getSlots());
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			inventory.setStackInSlot(slot, loaded.getStackInSlot(slot));
	}

	private CompoundTag writeInventory(HolderLookup.Provider registries) {
		CompoundTag serialized = new CompoundTag();
		serialized.putInt("Size", inventory.getSlots());

		ListTag items = new ListTag();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;

			int serializedSlot = slot;
			ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
				.result()
				.ifPresent(stackTag -> {
					if (stackTag instanceof CompoundTag item) {
						item.putByte("Slot", (byte) serializedSlot);
						items.add(item);
					}
				});
		}
		serialized.put("Items", items);
		return serialized;
	}

	@Override
	public void clearContent() {
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			ItemHelper.dropContents(level, pos, inventory);
			clearContent();
		}
		super.preRemoveSideEffects(pos, state);
	}

	@Override
	public void tick() {
		// Update Client block entity
		if (sendUpdate) {
			sendUpdate = false;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 6);
		}
	}

	public void startUpload(String schematic) {
		isUploading = true;
		uploadingProgress = 0;
		uploadingSchematic = schematic;
		sendUpdate = true;
		inventory.setStackInSlot(0, ItemStack.EMPTY);
	}

	public void finishUpload() {
		isUploading = false;
		uploadingProgress = 0;
		uploadingSchematic = null;
		sendUpdate = true;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return SchematicTableMenu.create(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return CreateLang.translateDirect("gui.schematicTable.title");
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
}
