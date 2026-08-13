package com.simibubi.create.content.logistics.depot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemSlots;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;

import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class DepotBehaviour extends BlockEntityBehaviour implements Clearable {
	public static final BehaviourType<DepotBehaviour> TYPE = new BehaviourType<>();

	TransportedItemStack heldItem;
	List<TransportedItemStack> incoming;
	ItemStackHandler processingOutputBuffer;
	public DepotItemHandler itemHandler;
	private ResourceHandler<ItemResource> itemResourceHandler;
	TransportedItemStackHandlerBehaviour transportedHandler;
	Supplier<Integer> maxStackSize;
	Supplier<Boolean> canAcceptItems;
	Predicate<Direction> canFunnelsPullFrom;
	Consumer<ItemStack> onHeldInserted;
	Predicate<ItemStack> acceptedItems;
	boolean allowMerge;

	public DepotBehaviour(SmartBlockEntity be) {
		super(be);
		maxStackSize = () -> heldItem != null ? heldItem.stack.getMaxStackSize() : 64;
		canAcceptItems = () -> true;
		canFunnelsPullFrom = $ -> true;
		acceptedItems = $ -> true;
		onHeldInserted = $ -> {
		};
		incoming = new ArrayList<>();
		itemHandler = new DepotItemHandler(this);
		processingOutputBuffer = new ItemStackHandler(8) {
			protected void onContentsChanged(int slot) {
				be.notifyUpdate();
			}
		};
	}

	public void enableMerging() {
		allowMerge = true;
	}

	public DepotBehaviour withCallback(Consumer<ItemStack> changeListener) {
		onHeldInserted = changeListener;
		return this;
	}

	public DepotBehaviour onlyAccepts(Predicate<ItemStack> filter) {
		acceptedItems = filter;
		return this;
	}

	public ResourceHandler<ItemResource> getItemResourceHandler() {
		if (itemResourceHandler == null)
			itemResourceHandler = new DepotResourceHandler();
		return itemResourceHandler;
	}

	@Override
	public void tick() {
		super.tick();

		Level world = blockEntity.getLevel();

		for (Iterator<TransportedItemStack> iterator = incoming.iterator(); iterator.hasNext(); ) {
			TransportedItemStack ts = iterator.next();
			if (!tick(ts))
				continue;
			if (world.isClientSide() && !blockEntity.isVirtual())
				continue;
			if (heldItem == null) {
				heldItem = ts;
			} else {
				if (!ItemHelper.canItemStackAmountsStack(heldItem.stack, ts.stack)) {
					Vec3 vec = VecHelper.getCenterOf(blockEntity.getBlockPos());
					Containers.dropItemStack(blockEntity.getLevel(), vec.x, vec.y + .5f, vec.z, ts.stack);
				} else {
					heldItem.stack.grow(ts.stack.getCount());
				}
			}
			iterator.remove();
			blockEntity.notifyUpdate();
		}

		if (heldItem == null)
			return;
		if (!tick(heldItem))
			return;

		BlockPos pos = blockEntity.getBlockPos();

		if (world.isClientSide())
			return;
		if (handleBeltFunnelOutput())
			return;

		BeltProcessingBehaviour processingBehaviour =
			BlockEntityBehaviour.get(world, pos.above(2), BeltProcessingBehaviour.TYPE);
		if (processingBehaviour == null)
			return;
		if (!heldItem.locked && BeltProcessingBehaviour.isBlocked(world, pos))
			return;

		ItemStack previousItem = heldItem.stack;
		boolean wasLocked = heldItem.locked;
		ProcessingResult result = wasLocked ? processingBehaviour.handleHeldItem(heldItem, transportedHandler)
			: processingBehaviour.handleReceivedItem(heldItem, transportedHandler);
		if (heldItem == null || result == ProcessingResult.REMOVE) {
			heldItem = null;
			blockEntity.sendData();
			return;
		}

		heldItem.locked = result == ProcessingResult.HOLD;
		if (heldItem.locked != wasLocked || !ItemStack.matches(previousItem, heldItem.stack))
			blockEntity.sendData();
	}

	protected boolean tick(TransportedItemStack heldItem) {
		heldItem.prevBeltPosition = heldItem.beltPosition;
		heldItem.prevSideOffset = heldItem.sideOffset;
		float diff = .5f - heldItem.beltPosition;
		if (diff > 1 / 512f) {
			if (diff > 1 / 32f && !BeltHelper.isItemUpright(heldItem.stack))
				heldItem.angle += 1;
			heldItem.beltPosition += diff / 4f;
		}
		return diff < 1 / 16f;
	}

	private boolean handleBeltFunnelOutput() {
		BlockState funnel = getWorld().getBlockState(getPos().above());
		Direction funnelFacing = AbstractFunnelBlock.getFunnelFacing(funnel);
		if (funnelFacing == null || !canFunnelsPullFrom.test(funnelFacing.getOpposite()))
			return false;

		for (int slot = 0; slot < processingOutputBuffer.getSlots(); slot++) {
			ItemStack previousItem = processingOutputBuffer.getStackInSlot(slot);
			if (previousItem.isEmpty())
				continue;
			ItemStack afterInsert = blockEntity.getBehaviour(DirectBeltInputBehaviour.TYPE)
				.tryExportingToBeltFunnel(previousItem, null, false);
			if (afterInsert == null)
				return false;
			if (previousItem.getCount() != afterInsert.getCount()) {
				processingOutputBuffer.setStackInSlot(slot, afterInsert);
				blockEntity.notifyUpdate();
				return true;
			}
		}

		ItemStack previousItem = heldItem.stack;
		ItemStack afterInsert = blockEntity.getBehaviour(DirectBeltInputBehaviour.TYPE)
			.tryExportingToBeltFunnel(previousItem, null, false);
		if (afterInsert == null)
			return false;
		if (previousItem.getCount() != afterInsert.getCount()) {
			if (afterInsert.isEmpty())
				heldItem = null;
			else
				heldItem.stack = afterInsert;
			blockEntity.notifyUpdate();
			return true;
		}

		return false;
	}

	@Override
	public void clearContent() {
		((ItemStackHandlerAccessor) processingOutputBuffer).create$getStacks().clear();
		incoming.clear();
		heldItem = null;
	}

	@Override
	public void destroy() {
		super.destroy();
		Level level = getWorld();
		BlockPos pos = getPos();
		ItemHelper.dropContents(level, pos, processingOutputBuffer);
		for (TransportedItemStack transportedItemStack : incoming)
			Block.popResource(level, pos, transportedItemStack.stack);
		if (!getHeldItemStack().isEmpty())
			Block.popResource(level, pos, getHeldItemStack());
	}

	@Override
	public void unload() {
		if (itemHandler != null || itemResourceHandler != null) {
			itemResourceHandler = null;
			blockEntity.invalidateCapabilities();
		}
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (heldItem != null)
			compound.put("HeldItem", heldItem.serializeNBT(registries));
		CatnipCodecUtils.encode(ItemSlots.CODEC, registries, ItemSlots.fromHandler(processingOutputBuffer))
			.ifPresent(tag -> compound.put("OutputBuffer", tag));
		if (canMergeItems() && !incoming.isEmpty())
			compound.put("Incoming", NBTHelper.writeCompoundList(incoming, stack -> stack.serializeNBT(registries)));
	}

	@Override
	public void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		heldItem = null;
		if (compound.contains("HeldItem"))
			heldItem = TransportedItemStack.read(compound.getCompoundOrEmpty("HeldItem"), registries);
		if (compound.contains("OutputBuffer")) {
			clearOutputBuffer();
			CatnipCodecUtils.decode(ItemSlots.CODEC, registries, compound.get("OutputBuffer"))
				.ifPresent(slots -> slots.forEach((slot, stack) -> {
					if (slot >= 0 && slot < processingOutputBuffer.getSlots())
						processingOutputBuffer.setStackInSlot(slot, stack);
				}));
		}
		if (canMergeItems()) {
			ListTag list = compound.getListOrEmpty("Incoming");
			incoming = NBTHelper.readCompoundList(list, c -> TransportedItemStack.read(c, registries));
		}
	}

	public void addSubBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(blockEntity).allowingBeltFunnels()
			.setInsertionHandler(this::tryInsertingFromSide).considerOccupiedWhen(this::isOccupied));
		transportedHandler = new TransportedItemStackHandlerBehaviour(blockEntity, this::applyToAllItems)
			.withStackPlacement(this::getWorldPositionOf);
		behaviours.add(transportedHandler);
	}

	public ItemStack getHeldItemStack() {
		return heldItem == null ? ItemStack.EMPTY : heldItem.stack;
	}

	public boolean canMergeItems() {
		return allowMerge;
	}

	public int getPresentStackSize() {
		int cumulativeStackSize = 0;
		cumulativeStackSize += getHeldItemStack().getCount();
		for (int slot = 0; slot < processingOutputBuffer.getSlots(); slot++)
			cumulativeStackSize += processingOutputBuffer.getStackInSlot(slot)
				.getCount();
		return cumulativeStackSize;
	}

	public int getRemainingSpace() {
		int cumulativeStackSize = getPresentStackSize();
		for (TransportedItemStack transportedItemStack : incoming)
			cumulativeStackSize += transportedItemStack.stack.getCount();
		int fromGetter =
			Math.min(maxStackSize.get() == 0 ? 64 : maxStackSize.get(), getHeldItemStack().getMaxStackSize());
		return (fromGetter) - cumulativeStackSize;
	}

	public ItemStack insert(TransportedItemStack heldItem, boolean simulate) {
		if (!canAcceptItems.get())
			return heldItem.stack;
		if (!acceptedItems.test(heldItem.stack))
			return heldItem.stack;

		if (canMergeItems()) {
			int remainingSpace = getRemainingSpace();
			ItemStack inserted = heldItem.stack;
			if (remainingSpace <= 0)
				return inserted;
			if (this.heldItem != null && !ItemHelper.canItemStackAmountsStack(this.heldItem.stack, inserted))
				return inserted;

			ItemStack returned = ItemStack.EMPTY;
			if (remainingSpace < inserted.getCount()) {
				returned = heldItem.stack.copyWithCount(inserted.getCount() - remainingSpace);
				if (!simulate) {
					TransportedItemStack copy = heldItem.copy();
					copy.stack.setCount(remainingSpace);
					if (this.heldItem != null)
						incoming.add(copy);
					else
						this.heldItem = copy;
				}
			} else {
				if (!simulate) {
					if (this.heldItem != null)
						incoming.add(heldItem);
					else
						this.heldItem = heldItem;
				}
			}
			return returned;
		}

		ItemStack returned = ItemStack.EMPTY;
		int maxCount = heldItem.stack.getMaxStackSize();
		boolean stackTooLarge = maxCount < heldItem.stack.getCount();
		if (stackTooLarge)
			returned = heldItem.stack.copyWithCount(heldItem.stack.getCount() - maxCount);

		if (simulate)
			return returned;

		if (this.isEmpty()) {
			if (heldItem.insertedFrom.getAxis().isHorizontal())
				AllSoundEvents.DEPOT_SLIDE.playOnServer(getWorld(), getPos());
			else
				AllSoundEvents.DEPOT_PLOP.playOnServer(getWorld(), getPos());
		}

		if (stackTooLarge) {
			heldItem = heldItem.copy();
			heldItem.stack.setCount(maxCount);
		}

		this.heldItem = heldItem;
		onHeldInserted.accept(heldItem.stack);
		return returned;
	}

	public void setHeldItem(TransportedItemStack heldItem) {
		this.heldItem = heldItem;
	}

	public void removeHeldItem() {
		this.heldItem = null;
	}

	private void clearOutputBuffer() {
		for (int slot = 0; slot < processingOutputBuffer.getSlots(); slot++)
			processingOutputBuffer.setStackInSlot(slot, ItemStack.EMPTY);
	}

	public void setCenteredHeldItem(TransportedItemStack heldItem) {
		this.heldItem = heldItem;
		this.heldItem.beltPosition = 0.5f;
		this.heldItem.prevBeltPosition = 0.5f;
	}

	private boolean isOccupied(Direction side) {
		if (!getHeldItemStack().isEmpty() && !canMergeItems())
			return true;
		if (!isOutputEmpty() && !canMergeItems())
			return true;
		if (!canAcceptItems.get())
			return true;
		return false;
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
		ItemStack inserted = transportedStack.stack;

		if (isOccupied(side))
			return inserted;

		int size = transportedStack.stack.getCount();
		transportedStack = transportedStack.copy();
		transportedStack.beltPosition = side.getAxis()
			.isVertical() ? .5f : 0;
		transportedStack.insertedFrom = side;
		transportedStack.prevSideOffset = transportedStack.sideOffset;
		transportedStack.prevBeltPosition = transportedStack.beltPosition;
		ItemStack remainder = insert(transportedStack, simulate);
		if (remainder.getCount() != size)
			blockEntity.notifyUpdate();

		return remainder;
	}

	private void applyToAllItems(float maxDistanceFromCentre,
								 Function<TransportedItemStack, TransportedResult> processFunction) {
		if (heldItem == null)
			return;
		if (.5f - heldItem.beltPosition > maxDistanceFromCentre)
			return;

		boolean dirty = false;
		TransportedItemStack transportedItemStack = heldItem;
		ItemStack stackBefore = transportedItemStack.stack.copy();
		TransportedResult result = processFunction.apply(transportedItemStack);
		if (result == null || result.didntChangeFrom(stackBefore))
			return;

		dirty = true;
		heldItem = null;
		if (result.hasHeldOutput())
			setCenteredHeldItem(result.getHeldOutput());

		for (TransportedItemStack added : result.getOutputs()) {
			if (getHeldItemStack().isEmpty()) {
				setCenteredHeldItem(added);
				continue;
			}
			ItemStack remainder = ItemHandlerHelper.insertItemStacked(processingOutputBuffer, added.stack, false);
			Vec3 vec = VecHelper.getCenterOf(blockEntity.getBlockPos());
			Containers.dropItemStack(blockEntity.getLevel(), vec.x, vec.y + .5f, vec.z, remainder);
		}

		if (dirty)
			blockEntity.notifyUpdate();
	}

	public boolean isEmpty() {
		return heldItem == null && isOutputEmpty();
	}

	public boolean isOutputEmpty() {
		for (int i = 0; i < processingOutputBuffer.getSlots(); i++)
			if (!processingOutputBuffer.getStackInSlot(i)
				.isEmpty())
				return false;
		return true;
	}

	private Vec3 getWorldPositionOf(TransportedItemStack transported) {
		return VecHelper.getCenterOf(blockEntity.getBlockPos());
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	public boolean isItemValid(ItemStack stack) {
		return acceptedItems.test(stack);
	}

	private class DepotResourceHandler implements ResourceHandler<ItemResource> {
		private final SnapshotJournal<DepotSnapshot> journal = new SnapshotJournal<>() {
			@Override
			protected DepotSnapshot createSnapshot() {
				List<TransportedItemStack> incomingSnapshot = new ArrayList<>();
				for (TransportedItemStack stack : incoming)
					incomingSnapshot.add(stack.copy());

				List<ItemStack> outputSnapshot = new ArrayList<>();
				for (int slot = 0; slot < processingOutputBuffer.getSlots(); slot++)
					outputSnapshot.add(processingOutputBuffer.getStackInSlot(slot)
						.copy());

				return new DepotSnapshot(heldItem == null ? null : heldItem.copy(), incomingSnapshot, outputSnapshot);
			}

			@Override
			protected void revertToSnapshot(DepotSnapshot snapshot) {
				heldItem = snapshot.heldItem == null ? null : snapshot.heldItem.copy();
				incoming.clear();
				for (TransportedItemStack stack : snapshot.incoming)
					incoming.add(stack.copy());
				for (int slot = 0; slot < snapshot.outputs.size(); slot++)
					processingOutputBuffer.setStackInSlot(slot, snapshot.outputs.get(slot)
						.copy());
				blockEntity.notifyUpdate();
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
			return resource.isEmpty() || itemHandler.isItemValid(index, resource.toStack(1));
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

	private record DepotSnapshot(TransportedItemStack heldItem, List<TransportedItemStack> incoming,
								 List<ItemStack> outputs) {
	}
}
