package com.simibubi.create.content.logistics.packagePort;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.animatedContainer.AnimatedContainerBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public abstract class PackagePortBlockEntity extends SmartBlockEntity implements MenuProvider, Clearable {
	public boolean acceptsPackages;
	public String addressFilter;
	public PackagePortTarget target;
	public SmartInventory inventory;

	protected AnimatedContainerBehaviour<PackagePortMenu> openTracker;

	protected IItemHandler itemHandler;
	protected ResourceHandler<ItemResource> itemResourceCapability;

	public PackagePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		addressFilter = "";
		acceptsPackages = true;
		inventory = new SmartInventory(18, this, (slot, stack) -> PackageItem.isPackage(stack));
		itemHandler = new PackagePortAutomationInventoryWrapper(inventory, this);
	}

	public boolean isBackedUp() {
		for (int i = 0; i < inventory.getSlots(); i++)
			if (inventory.getStackInSlot(i)
				.isEmpty())
				return false;
		return true;
	}

	public void filterChanged() {
		if (target != null) {
			target.deregister(this, level, worldPosition);
			target.register(this, level, worldPosition);
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (target != null)
			target.register(this, level, worldPosition);
	}

	public String getFilterString() {
		return acceptsPackages ? addressFilter : null;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (target != null)
			tag.put("Target", CatnipCodecUtils.encode(PackagePortTarget.CODEC, registries, target).orElseThrow());
		tag.putString("AddressFilter", addressFilter);
		tag.putBoolean("AcceptsPackages", acceptsPackages);
		tag.put("Inventory", inventory.serializeNBT(registries));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		inventory.deserializeNBT(registries, tag.getCompoundOrEmpty("Inventory"));
		PackagePortTarget prevTarget = target;
		target = CatnipCodecUtils.decodeOrNull(PackagePortTarget.CODEC, registries, tag.getCompoundOrEmpty("Target"));
		addressFilter = tag.getStringOr("AddressFilter", "");
		acceptsPackages = tag.getBooleanOr("AcceptsPackages", false);
		if (clientPacket && prevTarget != target)
			invalidateRenderBoundingBox();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		itemResourceCapability = null;
	}

	protected ResourceHandler<ItemResource> getItemResourceCapability() {
		if (itemResourceCapability == null)
			itemResourceCapability = new PackagePortResourceHandler();
		return itemResourceCapability;
	}

	@Override
	public void clearContent() {
		inventory.clearContent();
	}

	@Override
	public void destroy() {
		if (target != null)
			target.deregister(this, level, worldPosition);
		super.destroy();
		for (int i = 0; i < inventory.getSlots(); i++)
			drop(inventory.getStackInSlot(i));
	}

	public void drop(ItemStack box) {
		if (box.isEmpty())
			return;
		Block.popResource(level, worldPosition, box);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(openTracker = new AnimatedContainerBehaviour<>(this, PackagePortMenu.class));
		openTracker.onOpenChanged(this::onOpenChange);
	}

	protected abstract void onOpenChange(boolean open);

	public ItemInteractionResult use(Player player) {
		if (player == null || player.isCrouching())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (player instanceof FakePlayer)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		ItemStack mainHandItem = player.getMainHandItem();
		boolean clipboard = AllBlocks.CLIPBOARD.isIn(mainHandItem);

		if (level.isClientSide()) {
			if (!clipboard)
				onOpenedManually();
			return ItemInteractionResult.SUCCESS;
		}

		if (clipboard) {
			addAddressToClipboard(player, mainHandItem);
			return ItemInteractionResult.SUCCESS;
		}

		player.openMenu(this, worldPosition);
		return ItemInteractionResult.SUCCESS;
	}

	protected void onOpenedManually() {
	}

	private void addAddressToClipboard(Player player, ItemStack mainHandItem) {
		if (addressFilter == null || addressFilter.isBlank())
			return;

		ClipboardContent clipboard = mainHandItem.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
		List<List<ClipboardEntry>> list = ClipboardEntry.readAll(clipboard);
		for (List<ClipboardEntry> page : list) {
			for (ClipboardEntry entry : page) {
				String existing = entry.text.getString();
				if (existing.equals("#" + addressFilter) || existing.equals("# " + addressFilter))
					return;
			}
		}

		List<ClipboardEntry> page = null;

		for (List<ClipboardEntry> freePage : list) {
			if (freePage.size() > 11)
				continue;
			page = freePage;
			break;
		}

		if (page == null) {
			page = new ArrayList<>();
			list.add(page);
		}

		page.add(new ClipboardEntry(false, Component.literal("#" + addressFilter)));
		player.sendOverlayMessage(CreateLang.translateDirect("clipboard.address_added", addressFilter));


		clipboard = clipboard.setPages(list).setType(ClipboardType.WRITTEN);
		mainHandItem.set(AllDataComponents.CLIPBOARD_CONTENT, clipboard);
	}

	@Override
	public Component getDisplayName() {
		return Component.empty();
	}

	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return PackagePortMenu.create(pContainerId, pPlayerInventory, this);
	}

	public int getComparatorOutput() {
		return ItemHandlerHelper.calcRedstoneFromInventory(inventory);
	}

	private class PackagePortResourceHandler implements ResourceHandler<ItemResource> {
		private final SnapshotJournal<List<ItemStack>> journal = new SnapshotJournal<>() {
			@Override
			protected List<ItemStack> createSnapshot() {
				List<ItemStack> snapshot = new ArrayList<>();
				for (int slot = 0; slot < inventory.getSlots(); slot++)
					snapshot.add(inventory.getStackInSlot(slot)
						.copy());
				return snapshot;
			}

			@Override
			protected void revertToSnapshot(List<ItemStack> snapshot) {
				for (int slot = 0; slot < snapshot.size(); slot++)
					inventory.setStackInSlot(slot, snapshot.get(slot)
						.copy());
			}
		};

		@Override
		public int size() {
			return itemHandler.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(itemHandler.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return itemHandler.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return itemHandler.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (resource.isEmpty())
				return true;
			return itemHandler.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack remainder = itemHandler.insertItem(index, resource.toStack(amount), true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			itemHandler.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = itemHandler.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = itemHandler.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			itemHandler.extractItem(index, extracted.getCount(), false);
			return extracted.getCount();
		}
	}
}
