package com.simibubi.create.content.logistics.vault;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.foundation.item.ItemSlots;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.utility.SameSizeCombinedInvWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ItemVaultBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory, Clearable {
	protected ICapabilityProvider<IItemHandler> itemCapability = null;
	private ResourceHandler<ItemResource> itemResourceCapability;
	protected InventoryIdentifier invId;

	protected ItemStackHandler inventory;
	protected BlockPos controller;
	protected BlockPos lastKnownPos;
	protected boolean updateConnectivity;
	protected boolean loadingInventory;
	protected boolean queuedRemovalSplit;
	protected int radius;
	protected int length;

	public ItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

		inventory = new ItemStackHandler(AllConfigs.server().logistics.vaultCapacity.get()) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				if (loadingInventory || !hasLevel())
					return;
				updateComparators();
				level.blockEntityChanged(worldPosition);
			}
		};

		radius = 1;
		length = 1;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.ITEM_VAULT.get(),
			(be, side) -> be.getItemResourceCapability());
	}

	private ResourceHandler<ItemResource> getItemResourceCapability() {
		initCapability();
		if (itemCapability == null)
			return null;
		IItemHandler handler = itemCapability.getCapability();
		if (!(handler instanceof IItemHandlerModifiable modifiable))
			return null;
		if (itemResourceCapability == null)
			itemResourceCapability = new ItemHandlerResourceHandler(modifiable);
		return itemResourceCapability;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (!queuedRemovalSplit && hasLevel() && !level.isClientSide()) {
			queuedRemovalSplit = true;
			ItemHelper.dropContents(level, pos, inventory);
			ItemVaultBlock.prepareRemoval(this);
		}
		super.preRemoveSideEffects(pos, state);
	}

	protected void updateConnectivity() {
		updateConnectivity = false;
		if (!hasLevel())
			return;
		if (level.isClientSide())
			return;
		if (!isController())
			return;
		ConnectivityHandler.formMulti(this);
	}

	protected void updateComparators() {
		if (!hasLevel())
			return;
		ItemVaultBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return;

		level.blockEntityChanged(controllerBE.worldPosition);

		BlockPos pos = controllerBE.getBlockPos();

		int radius = controllerBE.radius;
		int length = controllerBE.length;

		Axis axis = controllerBE.getMainConnectionAxis();

		int zMax = (axis == Axis.X ? radius : length);
		int xMax = (axis == Axis.Z ? radius : length);

		// Mutable position we'll use for the blocks we poke updates at.
		MutableBlockPos updatePos = new MutableBlockPos();
		// Mutable position we'll set to be the vault block next to the update position.
		MutableBlockPos provokingPos = new MutableBlockPos();

		for (int y = 0; y < radius; y++) {
			for (int z = 0; z < zMax; z++) {
				for (int x = 0; x < xMax; x++) {
					// Emulate the effect of this line, but only for blocks along the surface of the vault:
					// level.updateNeighbourForOutputSignal(pos.offset(x, y, z), getBlockState().getBlock());
					// That method pokes all 6 directions in order. We want to preserve the update order
					// but skip the wasted work of checking other blocks that are part of this vault.

					var sectionX = SectionPos.blockToSectionCoord(pos.getX() + x);
					var sectionZ = SectionPos.blockToSectionCoord(pos.getZ() + z);
					if (!level.hasChunk(sectionX, sectionZ)) {
						continue;
					}
					provokingPos.setWithOffset(pos, x, y, z);

					// Technically all this work is wasted for the inner blocks of a long 3x3 vault, but
					// this is fast enough and relatively simple.
					Block provokingBlock = level.getBlockState(provokingPos).getBlock();

					// The 6 calls below should match the order of Direction.values().
					if (y == 0) {
						updateComaratorsInner(level, provokingBlock, provokingPos, updatePos, Direction.DOWN);
					}
					if (y == radius - 1) {
						updateComaratorsInner(level, provokingBlock, provokingPos, updatePos, Direction.UP);
					}
					if (z == 0) {
						updateComaratorsInner(level, provokingBlock, provokingPos, updatePos, Direction.NORTH);
					}
					if (z == zMax - 1) {
						updateComaratorsInner(level, provokingBlock, provokingPos, updatePos, Direction.SOUTH);
					}
					if (x == 0) {
						updateComaratorsInner(level, provokingBlock, provokingPos, updatePos, Direction.WEST);
					}
					if (x == xMax - 1) {
						updateComaratorsInner(level, provokingBlock, provokingPos, updatePos, Direction.EAST);
					}
				}
			}
		}
	}

	/**
	 * See {@link Level#updateNeighbourForOutputSignal(BlockPos, Block)}.
	 */
	private static void updateComaratorsInner(Level level, Block provokingBlock, BlockPos provokingPos, MutableBlockPos updatePos, Direction direction) {
		updatePos.setWithOffset(provokingPos, direction);

		var sectionX = SectionPos.blockToSectionCoord(updatePos.getX());
		var sectionZ = SectionPos.blockToSectionCoord(updatePos.getZ());
		if (!level.hasChunk(sectionX, sectionZ)) {
			return;
		}

		BlockState blockstate = level.getBlockState(updatePos);
		blockstate.onNeighborChange(level, updatePos, provokingPos);
		if (blockstate.isRedstoneConductor(level, updatePos)) {
			updatePos.move(direction);
			blockstate = level.getBlockState(updatePos);
			if (blockstate.getWeakChanges(level, updatePos)) {
				level.neighborChanged(blockstate, updatePos, provokingBlock, null, false);
			}
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (lastKnownPos == null)
			lastKnownPos = getBlockPos();
		else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
			onPositionChanged();
			return;
		}

		if (updateConnectivity)
			updateConnectivity();
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public boolean isController() {
		return controller == null || worldPosition.getX() == controller.getX()
			&& worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
	}

	private void onPositionChanged() {
		removeController(true);
		lastKnownPos = worldPosition;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ItemVaultBlockEntity getControllerBE() {
		if (isController())
			return this;
		if (!hasLevel() || controller == null)
			return null;
		BlockEntity blockEntity = level.getBlockEntity(controller);
		if (blockEntity instanceof ItemVaultBlockEntity)
			return (ItemVaultBlockEntity) blockEntity;
		return null;
	}

	public void removeController(boolean keepContents) {
		if (level.isClientSide())
			return;
		updateConnectivity = true;
		controller = null;
		radius = 1;
		length = 1;

		BlockState state = getBlockState();
		if (ItemVaultBlock.isVault(state)) {
			state = state.setValue(ItemVaultBlock.LARGE, false);
			getLevel().setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
		}

		itemCapability = null;
		itemResourceCapability = null;
		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public void setController(BlockPos controller) {
		if (level.isClientSide() && !isVirtual())
			return;
		if (controller.equals(this.controller))
			return;
		this.controller = controller;
		itemCapability = null;
		itemResourceCapability = null;
		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);

		BlockPos controllerBefore = controller;
		int prevSize = radius;
		int prevLength = length;

		updateConnectivity = compound.contains("Uninitialized");

		lastKnownPos = null;
		if (compound.contains("LastKnownPos"))
			lastKnownPos = readBlockPos(compound.get("LastKnownPos"));

		controller = null;
		if (compound.contains("Controller"))
			controller = readBlockPos(compound.get("Controller"));

		if (isController()) {
			radius = compound.getIntOr("Size", 1);
			length = compound.getIntOr("Length", 1);
		}

		if (!clientPacket) {
			deserializeInventory(registries, inventory, compound.get("Inventory"));
			return;
		}

		boolean changeOfController =
			controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
		if (hasLevel() && (changeOfController || prevSize != radius || prevLength != length))
			level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (updateConnectivity)
			compound.putBoolean("Uninitialized", true);

		if (lastKnownPos != null)
			compound.put("LastKnownPos", writeBlockPos(lastKnownPos));
		if (!isController())
			compound.put("Controller", writeBlockPos(controller));
		if (isController()) {
			compound.putInt("Size", radius);
			compound.putInt("Length", length);
		}

		super.write(compound, registries, clientPacket);

		if (!clientPacket) {
			compound.putString("StorageType", "CombinedInv");
			serializeInventory(registries, inventory).ifPresent(tag -> compound.put("Inventory", tag));
		}
	}

	@Override
	public void clearContent() {
		List<ItemStack> stacks = ((ItemStackHandlerAccessor) inventory).create$getStacks();
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			stacks.set(slot, ItemStack.EMPTY);
		updateComparators();
	}

	public ItemStackHandler getInventoryOfBlock() {
		return inventory;
	}

	public InventoryIdentifier getInvId() {
		// ensure capability is up to date first, which sets the ID
		this.initCapability();
		return this.invId;
	}

	public void applyInventoryToBlock(ItemStackHandler handler) {
		loadingInventory = true;
		try {
			for (int i = 0; i < inventory.getSlots(); i++)
				inventory.setStackInSlot(i, i < handler.getSlots() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
		} finally {
			loadingInventory = false;
		}
	}

	private void initCapability() {
		if (!hasLevel())
			return;
		if (itemCapability != null && itemCapability.getCapability() != null)
			return;
		if (!isController()) {
			ItemVaultBlockEntity controllerBE = getControllerBE();
			if (controllerBE == null)
				return;
			controllerBE.initCapability();
			itemCapability = ICapabilityProvider.of(() -> {
				if (controllerBE.isRemoved())
					return null;
				if (controllerBE.itemCapability == null)
					return null;
				return controllerBE.itemCapability.getCapability();
			});
			invId = controllerBE.invId;
			return;
		}

		boolean alongZ = ItemVaultBlock.getVaultBlockAxis(getBlockState()) == Axis.Z;
		IItemHandlerModifiable[] invs = new IItemHandlerModifiable[length * radius * radius];
		for (int yOffset = 0; yOffset < length; yOffset++) {
			for (int xOffset = 0; xOffset < radius; xOffset++) {
				for (int zOffset = 0; zOffset < radius; zOffset++) {
					BlockPos vaultPos = alongZ ? worldPosition.offset(xOffset, zOffset, yOffset)
						: worldPosition.offset(yOffset, xOffset, zOffset);
					ItemVaultBlockEntity vaultAt =
						ConnectivityHandler.partAt(AllBlockEntityTypes.ITEM_VAULT.get(), level, vaultPos);
					invs[yOffset * radius * radius + xOffset * radius + zOffset] =
						vaultAt != null ? vaultAt.inventory : new ItemStackHandler();
				}
			}
		}

		itemCapability = ICapabilityProvider.of(new VersionedInventoryWrapper(SameSizeCombinedInvWrapper.create(invs)));

		// build an identifier encompassing all component vaults
		BlockPos farCorner = alongZ
			? worldPosition.offset(radius, radius, length)
			: worldPosition.offset(length, radius, radius);
		BoundingBox bounds = BoundingBox.fromCorners(this.worldPosition, farCorner);
		this.invId = new InventoryIdentifier.Bounds(bounds);
	}

	public static int getMaxLength(int radius) {
		return radius * 3;
	}

	@Override
	public void preventConnectivityUpdate() { updateConnectivity = false; }

	@Override
	public void notifyMultiUpdated() {
		BlockState state = this.getBlockState();
		if (ItemVaultBlock.isVault(state)) { // safety
			BlockState updatedState = state.setValue(ItemVaultBlock.LARGE, radius > 2);
			level.setBlock(getBlockPos(), updatedState, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
			if (updatedState == state)
				level.sendBlockUpdated(getBlockPos(), state, updatedState, Block.UPDATE_CLIENTS);
		}
		itemCapability = null;
		itemResourceCapability = null;
		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() { return getMainAxisOf(this); }

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		if (longAxis == Direction.Axis.Y) return getMaxWidth();
		return getMaxLength(width);
	}

	@Override
	public int getMaxWidth() {
		return 3;
	}

	@Override
	public int getHeight() { return length; }

	@Override
	public int getWidth() { return radius; }

	@Override
	public void setHeight(int height) { this.length = height; }

	@Override
	public void setWidth(int width) { this.radius = width; }

	@Override
	public boolean hasInventory() { return true; }

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readBlockPos(Tag tag) {
		if (tag instanceof CompoundTag compound)
			return new BlockPos(compound.getIntOr("X", 0), compound.getIntOr("Y", 0), compound.getIntOr("Z", 0));
		return BlockPos.CODEC.parse(NbtOps.INSTANCE, tag)
			.result()
			.orElse(BlockPos.ZERO);
	}

	static java.util.Optional<Tag> serializeInventory(HolderLookup.Provider registries, ItemStackHandler handler) {
		return ItemSlots.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), ItemSlots.fromHandler(handler))
			.result();
	}

	static void deserializeInventory(HolderLookup.Provider registries, ItemStackHandler handler, Tag tag) {
		if (tag == null)
			return;
		ItemSlots.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
			.result()
			.map(result -> result.getFirst())
			.ifPresent(slots -> {
				List<ItemStack> stacks = ((ItemStackHandlerAccessor) handler).create$getStacks();
				for (int slot = 0; slot < handler.getSlots(); slot++)
					stacks.set(slot, ItemStack.EMPTY);
				slots.forEach((slot, stack) -> {
					if (slot < handler.getSlots())
						stacks.set(slot, stack);
				});
			});
	}

	private static class ItemHandlerResourceHandler implements ResourceHandler<ItemResource> {
		private final IItemHandlerModifiable handler;
		private final List<SlotJournal> journals;

		private ItemHandlerResourceHandler(IItemHandlerModifiable handler) {
			this.handler = handler;
			journals = new ArrayList<>();
			for (int slot = 0; slot < handler.getSlots(); slot++)
				journals.add(new SlotJournal(slot));
		}

		@Override
		public int size() {
			return handler.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(handler.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return handler.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return handler.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (resource.isEmpty())
				return true;
			return handler.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack stack = resource.toStack(amount);
			ItemStack remainder = handler.insertItem(index, stack, true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journals.get(index)
				.updateSnapshots(transaction);
			handler.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = handler.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = handler.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journals.get(index)
				.updateSnapshots(transaction);
			handler.extractItem(index, extracted.getCount(), false);
			return extracted.getCount();
		}

		private class SlotJournal extends SnapshotJournal<ItemStack> {
			private final int slot;

			private SlotJournal(int slot) {
				this.slot = slot;
			}

			@Override
			protected ItemStack createSnapshot() {
				return handler.getStackInSlot(slot)
					.copy();
			}

			@Override
			protected void revertToSnapshot(ItemStack snapshot) {
				handler.setStackInSlot(slot, snapshot);
			}
		}
	}
}
