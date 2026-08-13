package com.simibubi.create.content.fluids.drain;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankResourceHandler;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ItemDrainBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Clearable {

	public static final int FILLING_TIME = 20;

	SmartFluidTankBehaviour internalTank;
	TransportedItemStack heldItem;
	protected int processingTicks;
	Map<Direction, ItemDrainItemHandler> itemHandlers;
	private ResourceHandler<FluidResource> fluidResourceCapability;
	private Map<Direction, ResourceHandler<ItemResource>> itemResourceCapabilities;

	public ItemDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		itemHandlers = new IdentityHashMap<>();
		itemResourceCapabilities = new IdentityHashMap<>();
		for (Direction d : Iterate.horizontalDirections) {
			itemHandlers.put(d, new ItemDrainItemHandler(this, d));
			itemResourceCapabilities.put(d, new ItemDrainItemResourceHandler(d));
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide() && heldItem != null && !heldItem.stack.isEmpty()) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), heldItem.stack);
			heldItem = null;
		}
		super.preRemoveSideEffects(pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.ITEM_DRAIN.get(),
			(be, side) -> side != null && side.getAxis()
				.isHorizontal() ? be.itemResourceCapabilities.get(side) : null);
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, AllBlockEntityTypes.ITEM_DRAIN.get(),
			(be, side) -> side != Direction.UP ? be.getFluidResourceCapability() : null);
	}

	protected ResourceHandler<FluidResource> getFluidResourceCapability() {
		if (fluidResourceCapability == null)
			fluidResourceCapability = new SmartFluidTankResourceHandler(internalTank);
		return fluidResourceCapability;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnels()
			.setInsertionHandler(this::tryInsertingFromSide));
		behaviours.add(internalTank = SmartFluidTankBehaviour.single(this, 1500)
			.allowExtraction()
			.forbidInsertion());
		registerAwardables(behaviours, AllAdvancements.DRAIN, AllAdvancements.CHAINED_DRAIN);
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
		ItemStack inserted = transportedStack.stack;
		ItemStack returned = ItemStack.EMPTY;

		if (!getHeldItemStack().isEmpty())
			return inserted;

		if (inserted.getCount() > 1 && GenericItemEmptying.canItemBeEmptied(level, inserted)) {
			returned = inserted.copyWithCount(inserted.getCount() - 1);
			inserted = inserted.copyWithCount(1);
		}

		if (simulate)
			return returned;

		transportedStack = transportedStack.copy();
		transportedStack.stack = inserted.copy();
		transportedStack.beltPosition = side.getAxis()
			.isVertical() ? .5f : 0;
		transportedStack.prevSideOffset = transportedStack.sideOffset;
		transportedStack.prevBeltPosition = transportedStack.beltPosition;
		setHeldItem(transportedStack, side);
		setChanged();
		sendData();

		return returned;
	}

	public ItemStack getHeldItemStack() {
		return heldItem == null ? ItemStack.EMPTY : heldItem.stack;
	}

	@Override
	public void tick() {
		super.tick();

		if (heldItem == null) {
			processingTicks = 0;
			return;
		}

		boolean onClient = level.isClientSide() && !isVirtual();

		if (processingTicks > 0) {
			heldItem.prevBeltPosition = .5f;
			boolean wasAtBeginning = processingTicks == FILLING_TIME;
			if (!onClient || processingTicks < FILLING_TIME)
				processingTicks--;
			if (!continueProcessing()) {
				processingTicks = 0;
				notifyUpdate();
				return;
			}
			if (wasAtBeginning != (processingTicks == FILLING_TIME))
				sendData();
			return;
		}

		heldItem.prevBeltPosition = heldItem.beltPosition;
		heldItem.prevSideOffset = heldItem.sideOffset;

		heldItem.beltPosition += itemMovementPerTick();
		if (heldItem.beltPosition > 1) {
			heldItem.beltPosition = 1;

			if (onClient)
				return;

			Direction side = heldItem.insertedFrom;

			ItemStack tryExportingToBeltFunnel = getBehaviour(DirectBeltInputBehaviour.TYPE)
				.tryExportingToBeltFunnel(heldItem.stack, side.getOpposite(), false);
			if (tryExportingToBeltFunnel != null) {
				if (tryExportingToBeltFunnel.getCount() != heldItem.stack.getCount()) {
					if (tryExportingToBeltFunnel.isEmpty())
						heldItem = null;
					else
						heldItem.stack = tryExportingToBeltFunnel;
					notifyUpdate();
					return;
				}
				if (!tryExportingToBeltFunnel.isEmpty())
					return;
			}

			BlockPos nextPosition = worldPosition.relative(side);
			DirectBeltInputBehaviour directBeltInputBehaviour =
				BlockEntityBehaviour.get(level, nextPosition, DirectBeltInputBehaviour.TYPE);
			if (directBeltInputBehaviour == null) {
				if (!BlockHelper.hasBlockSolidSide(level.getBlockState(nextPosition), level, nextPosition,
					side.getOpposite())) {
					ItemStack ejected = heldItem.stack;
					Vec3 outPos = VecHelper.getCenterOf(worldPosition)
						.add(Vec3.atLowerCornerOf(side.getUnitVec3i())
							.scale(.75));
					float movementSpeed = itemMovementPerTick();
					Vec3 outMotion = Vec3.atLowerCornerOf(side.getUnitVec3i())
						.scale(movementSpeed)
						.add(0, 1 / 8f, 0);
					outPos.add(outMotion.normalize());
					ItemEntity entity = new ItemEntity(level, outPos.x, outPos.y + 6 / 16f, outPos.z, ejected);
					entity.setDeltaMovement(outMotion);
					entity.setDefaultPickUpDelay();
					entity.hurtMarked = true;
					level.addFreshEntity(entity);

					heldItem = null;
					notifyUpdate();
				}
				return;
			}

			if (!directBeltInputBehaviour.canInsertFromSide(side))
				return;

			ItemStack returned = directBeltInputBehaviour.handleInsertion(heldItem.copy(), side, false);

			if (returned.isEmpty()) {
				if (level.getBlockEntity(nextPosition) instanceof ItemDrainBlockEntity)
					award(AllAdvancements.CHAINED_DRAIN);
				heldItem = null;
				notifyUpdate();
				return;
			}

			if (returned.getCount() != heldItem.stack.getCount()) {
				heldItem.stack = returned;
				notifyUpdate();
				return;
			}

			return;
		}

		if (heldItem.prevBeltPosition < .5f && heldItem.beltPosition >= .5f) {
			if (!GenericItemEmptying.canItemBeEmptied(level, heldItem.stack))
				return;
			heldItem.beltPosition = .5f;
			if (onClient)
				return;
			processingTicks = FILLING_TIME;
			sendData();
		}

	}

	protected boolean continueProcessing() {
		if (level.isClientSide() && !isVirtual())
			return true;
		if (processingTicks < 5)
			return true;
		if (!GenericItemEmptying.canItemBeEmptied(level, heldItem.stack))
			return false;

		Pair<FluidStack, ItemStack> emptyItem = GenericItemEmptying.emptyItem(level, heldItem.stack, true);
		FluidStack fluidFromItem = emptyItem.getFirst();

		if (processingTicks > 5) {
			internalTank.allowInsertion();
			if (internalTank.getPrimaryHandler()
				.fill(fluidFromItem, FluidAction.SIMULATE) != fluidFromItem.getAmount()) {
				internalTank.forbidInsertion();
				processingTicks = FILLING_TIME;
				return true;
			}
			internalTank.forbidInsertion();
			return true;
		}

		emptyItem = GenericItemEmptying.emptyItem(level, heldItem.stack.copy(), false);
		award(AllAdvancements.DRAIN);

		// Process finished
		ItemStack out = emptyItem.getSecond();
		if (!out.isEmpty())
			heldItem.stack = out;
		else
			heldItem = null;
		internalTank.allowInsertion();
		internalTank.getPrimaryHandler()
			.fill(fluidFromItem, FluidAction.EXECUTE);
		internalTank.forbidInsertion();
		notifyUpdate();
		return true;
	}

	private float itemMovementPerTick() {
		return 1 / 8f;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

	public void setHeldItem(TransportedItemStack heldItem, Direction insertedFrom) {
		this.heldItem = heldItem;
		this.heldItem.insertedFrom = insertedFrom;
	}

	@Override
	public void clearContent() {
		this.heldItem = null;
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		if (heldItem != null)
			compound.put("HeldItem", heldItem.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		heldItem = null;
		processingTicks = compound.getIntOr("ProcessingTicks", 0);
		if (compound.contains("HeldItem"))
			heldItem = TransportedItemStack.read(compound.getCompoundOrEmpty("HeldItem"), registries);
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return containedFluidTooltip(tooltip, isPlayerSneaking, internalTank.getPrimaryHandler());
	}

	private class ItemDrainItemResourceHandler implements ResourceHandler<ItemResource> {
		private final Direction side;
		private final HeldItemJournal journal;

		private ItemDrainItemResourceHandler(Direction side) {
			this.side = side;
			journal = new HeldItemJournal();
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(getHeldItemStack());
		}

		@Override
		public long getAmountAsLong(int index) {
			return getHeldItemStack().getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			return 64;
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return true;
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0 || !getHeldItemStack().isEmpty())
				return 0;

			ItemStack stack = resource.toStack(amount);
			ItemStack remainder = itemHandlers.get(side)
				.insertItem(index, stack, true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			itemHandlers.get(side)
				.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack heldStack = getHeldItemStack();
			if (!resource.matches(heldStack))
				return 0;

			ItemStack extracted = itemHandlers.get(side)
				.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			itemHandlers.get(side)
				.extractItem(index, extracted.getCount(), false);
			return extracted.getCount();
		}

		private class HeldItemJournal extends SnapshotJournal<TransportedItemStack> {
			@Override
			protected TransportedItemStack createSnapshot() {
				return heldItem == null ? null : heldItem.copy();
			}

			@Override
			protected void revertToSnapshot(TransportedItemStack snapshot) {
				heldItem = snapshot == null ? null : snapshot.copy();
				notifyUpdate();
			}
		}
	}

}
