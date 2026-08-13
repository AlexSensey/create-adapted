package com.simibubi.create.content.kinetics.belt;

import static com.simibubi.create.content.kinetics.belt.BeltPart.MIDDLE;
import static com.simibubi.create.content.kinetics.belt.BeltSlope.HORIZONTAL;
import static net.minecraft.core.Direction.AxisDirection.NEGATIVE;
import static net.minecraft.core.Direction.AxisDirection.POSITIVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler;
import com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class BeltBlockEntity extends KineticBlockEntity implements Clearable {
	private static boolean syncingBeltSpeeds;

	public Map<Entity, TransportedEntityInfo> passengers;
	public Optional<DyeColor> color;
	public int beltLength;
	public int index;
	public Direction lastInsert;
	public CasingType casing;
	public boolean covered;

	protected BlockPos controller;
	protected BeltInventory inventory;
	protected IItemHandler itemHandler;
	protected ResourceHandler<ItemResource> itemResourceHandler;
	public VersionedInventoryTrackerBehaviour invVersionTracker;

	public CompoundTag trackerUpdateTag;

	public static enum CasingType {
		NONE, ANDESITE, BRASS;
	}

	public BeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		controller = BlockPos.ZERO;
		itemHandler = null;
		casing = CasingType.NONE;
		color = Optional.empty();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.BELT.get(),
			(be, side) -> be.getItemResourceHandler());
	}

	private ResourceHandler<ItemResource> getItemResourceHandler() {
		initializeItemHandler();
		if (itemResourceHandler == null)
			itemResourceHandler = new BeltSegmentResourceHandler();
		return itemResourceHandler;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::canInsertFrom)
			.setInsertionHandler(this::tryInsertingFromSide).considerOccupiedWhen(this::isOccupied));
		behaviours.add(new TransportedItemStackHandlerBehaviour(this, this::applyToAllItems)
			.withStackPlacement(this::getWorldPositionOf));
		behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
	}

	@Override
	public void tick() {
		// Init belt
		if (beltLength == 0)
			BeltBlock.initBelt(level, worldPosition);

		super.tick();

		if (!AllBlocks.BELT.has(level.getBlockState(worldPosition)))
			return;

		initializeItemHandler();

		// Move Items
		if (!isController())
			return;

		invalidateRenderBoundingBox();

		getInventory().tick();

		if (getSpeed() == 0)
			return;

		// Move Entities
		if (passengers == null)
			passengers = new HashMap<>();

		List<Entity> toRemove = new ArrayList<>();
		passengers.forEach((entity, info) -> {
			boolean canBeTransported = BeltMovementHandler.canBeTransported(entity);
			boolean leftTheBelt =
				info.getTicksSinceLastCollision() > ((getBlockState().getValue(BeltBlock.SLOPE) != HORIZONTAL) ? 3 : 1);
			if (!canBeTransported || leftTheBelt) {
				toRemove.add(entity);
				return;
			}

			info.tick();
			BeltMovementHandler.transportEntity(this, entity, info);
		});
		toRemove.forEach(passengers::remove);
	}

	@Override
	public float calculateStressApplied() {
		if (!isController())
			return 0;
		return super.calculateStressApplied();
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level == null || level.isClientSide())
			return;
		if (syncingBeltSpeeds)
			return;
		if (beltLength == 0)
			return;

		syncingBeltSpeeds = true;
		try {
			float newSpeed = getTheoreticalSpeed();
			for (BlockPos beltPos : BeltBlock.getBeltChain(level, getController())) {
				BlockEntity blockEntity = level.getBlockEntity(beltPos);
				if (!(blockEntity instanceof BeltBlockEntity belt))
					continue;
				if (belt == this)
					continue;
				if (!getController().equals(belt.getController()))
					continue;

				float prevSpeed = belt.getSpeed();
				if (newSpeed == 0) {
					belt.removeSource();
				} else {
					belt.setSpeed(newSpeed);
					belt.setSource(getBlockPos());
					belt.onSpeedChanged(prevSpeed);
				}
				belt.sendData();
			}
		} finally {
			syncingBeltSpeeds = false;
		}
	}

	@Override
	public AABB createRenderBoundingBox() {
		if (!isController())
			return super.createRenderBoundingBox();
		else
			return super.createRenderBoundingBox().inflate(beltLength + 1);
	}

	protected void initializeItemHandler() {
		if (level.isClientSide() || itemHandler != null)
			return;
		if (beltLength == 0 || controller == null)
			return;
		if (!level.isLoaded(controller))
			return;
		BlockEntity be = level.getBlockEntity(controller);
		if (be == null || !(be instanceof BeltBlockEntity))
			return;
		BeltInventory inventory = ((BeltBlockEntity) be).getInventory();
		if (inventory == null)
			return;
		itemHandler = new ItemHandlerBeltSegment(inventory, index);
		itemResourceHandler = null;
		invalidateCapabilities();
	}

	@Override
	public void clearContent() {
		if (inventory != null) {
			inventory.getTransportedItems().clear();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (isController())
			getInventory().ejectAll();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		itemResourceHandler = null;
		invalidateCapabilities();
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (controller != null)
			compound.put("Controller", writeBlockPos(controller));
		compound.putBoolean("IsController", isController());
		compound.putInt("Length", beltLength);
		compound.putInt("Index", index);
		compound.putString("Casing", casing.name());
		compound.putBoolean("Covered", covered);

		color.ifPresent(dyeColor -> compound.putString("Dye", dyeColor.name()));

		if (isController())
			compound.put("Inventory", getInventory().write(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);

		if (compound.getBooleanOr("IsController", false))
			controller = worldPosition;

		color = compound.contains("Dye") ? Optional.of(readEnum(compound, "Dye", DyeColor.class, DyeColor.WHITE))
			: Optional.empty();

		if (!wasMoved) {
			if (!isController())
				controller = readBlockPos(compound.getCompoundOrEmpty("Controller"));
			trackerUpdateTag = compound;
			index = compound.getIntOr("Index", 0);
			beltLength = compound.getIntOr("Length", 0);
		}

		if (isController())
			getInventory().read(compound.getCompoundOrEmpty("Inventory"), registries);

		CasingType casingBefore = casing;
		boolean coverBefore = covered;
		casing = readEnum(compound, "Casing", CasingType.class, CasingType.NONE);
		covered = compound.getBooleanOr("Covered", false);

		if (!clientPacket)
			return;

		if (casingBefore == casing && coverBefore == covered)
			return;
		if (!isVirtual())
			requestModelDataUpdate();
		if (hasLevel())
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readBlockPos(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
	}

	private static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, Class<E> enumClass, E fallback) {
		try {
			return Enum.valueOf(enumClass, tag.getStringOr(key, fallback.name()));
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	@Override
	public void clearKineticInformation() {
		super.clearKineticInformation();
		beltLength = 0;
		index = 0;
		controller = null;
		trackerUpdateTag = new CompoundTag();
	}

	public boolean applyColor(DyeColor colorIn) {
		if (colorIn == null) {
			if (!color.isPresent())
				return false;
		} else if (color.isPresent() && color.get() == colorIn)
			return false;
		if (level.isClientSide())
			return true;

		for (BlockPos blockPos : BeltBlock.getBeltChain(level, getController())) {
			BeltBlockEntity belt = BeltHelper.getSegmentBE(level, blockPos);
			if (belt == null)
				continue;
			belt.color = Optional.ofNullable(colorIn);
			belt.setChanged();
			belt.sendData();
		}

		return true;
	}

	public BeltBlockEntity getControllerBE() {
		if (controller == null)
			return null;
		if (!level.isLoaded(controller))
			return null;
		BlockEntity be = level.getBlockEntity(controller);
		if (be == null || !(be instanceof BeltBlockEntity))
			return null;
		return (BeltBlockEntity) be;
	}

	public void setController(BlockPos controller) {
		this.controller = controller;
	}

	public BlockPos getController() {
		return controller == null ? worldPosition : controller;
	}

	public boolean isController() {
		return controller != null && worldPosition.getX() == controller.getX()
			&& worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
	}

	public float getBeltMovementSpeed() {
		return getSpeed() / 480f;
	}

	public float getDirectionAwareBeltMovementSpeed() {
		int offset = getBeltFacing().getAxisDirection()
			.getStep();
		if (getBeltFacing().getAxis() == Axis.X)
			offset *= -1;
		return getBeltMovementSpeed() * offset;
	}

	public boolean hasPulley() {
		if (!AllBlocks.BELT.has(getBlockState()))
			return false;
		return getBlockState().getValue(BeltBlock.PART) != MIDDLE;
	}

	protected boolean isLastBelt() {
		if (getSpeed() == 0)
			return false;

		Direction direction = getBeltFacing();
		if (getBlockState().getValue(BeltBlock.SLOPE) == BeltSlope.VERTICAL)
			return false;

		BeltPart part = getBlockState().getValue(BeltBlock.PART);
		if (part == MIDDLE)
			return false;

		boolean movingPositively = (getSpeed() > 0 == (direction.getAxisDirection()
			.getStep() == 1)) ^ direction.getAxis() == Axis.X;
		return part == BeltPart.START ^ movingPositively;
	}

	public Vec3i getMovementDirection(boolean firstHalf) {
		return this.getMovementDirection(firstHalf, false);
	}

	public Vec3i getBeltChainDirection() {
		return this.getMovementDirection(true, true);
	}

	protected Vec3i getMovementDirection(boolean firstHalf, boolean ignoreHalves) {
		if (getSpeed() == 0)
			return BlockPos.ZERO;

		final BlockState blockState = getBlockState();
		final Direction beltFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
		final BeltSlope slope = blockState.getValue(BeltBlock.SLOPE);
		final BeltPart part = blockState.getValue(BeltBlock.PART);
		final Axis axis = beltFacing.getAxis();

		Direction movementFacing = Direction.get(axis == Axis.X ? NEGATIVE : POSITIVE, axis);
		boolean notHorizontal = blockState.getValue(BeltBlock.SLOPE) != HORIZONTAL;
		if (getSpeed() < 0)
			movementFacing = movementFacing.getOpposite();
		Vec3i movement = movementFacing.getUnitVec3i();

		boolean slopeBeforeHalf = (part == BeltPart.END) == (beltFacing.getAxisDirection() == POSITIVE);
		boolean onSlope = notHorizontal && (part == MIDDLE || slopeBeforeHalf == firstHalf || ignoreHalves);
		boolean movingUp = onSlope && slope == (movementFacing == beltFacing ? BeltSlope.UPWARD : BeltSlope.DOWNWARD);

		if (!onSlope)
			return movement;

		return new Vec3i(movement.getX(), movingUp ? 1 : -1, movement.getZ());
	}

	public Direction getMovementFacing() {
		Axis axis = getBeltFacing().getAxis();
		return Direction.fromAxisAndDirection(axis, getBeltMovementSpeed() < 0 ^ axis == Axis.X ? NEGATIVE : POSITIVE);
	}

	protected Direction getBeltFacing() {
		return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
	}

	public BeltInventory getInventory() {
		if (!isController()) {
			BeltBlockEntity controllerBE = getControllerBE();
			if (controllerBE != null)
				return controllerBE.getInventory();
			return null;
		}
		if (inventory == null) {
			inventory = new BeltInventory(this);
		}
		return inventory;
	}

	private void applyToAllItems(float maxDistanceFromCenter,
								 Function<TransportedItemStack, TransportedResult> processFunction) {
		BeltBlockEntity controller = getControllerBE();
		if (controller == null)
			return;
		BeltInventory inventory = controller.getInventory();
		if (inventory != null)
			inventory.applyToEachWithin(index + .5f, maxDistanceFromCenter, processFunction);
	}

	private Vec3 getWorldPositionOf(TransportedItemStack transported) {
		BeltBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return Vec3.ZERO;
		return BeltHelper.getVectorForOffset(controllerBE, transported.beltPosition);
	}

	public void setCasingType(CasingType type) {
		if (casing == type)
			return;

		BlockState blockState = getBlockState();
		boolean shouldBlockHaveCasing = type != CasingType.NONE;

		if (level.isClientSide()) {
			casing = type;
			level.setBlock(worldPosition, blockState.setValue(BeltBlock.CASING, shouldBlockHaveCasing), 0);
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
			return;
		}

		if (casing != CasingType.NONE)
			level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, worldPosition,
				Block.getId(casing == CasingType.ANDESITE ? AllBlocks.ANDESITE_CASING.getDefaultState()
					: AllBlocks.BRASS_CASING.getDefaultState()));
		if (blockState.getValue(BeltBlock.CASING) != shouldBlockHaveCasing)
			KineticBlockEntity.switchToBlockState(level, worldPosition,
				blockState.setValue(BeltBlock.CASING, shouldBlockHaveCasing));
		casing = type;
		setChanged();
		sendData();
	}

	private boolean canInsertFrom(Direction side) {
		if (getSpeed() == 0)
			return false;
		BlockState state = getBlockState();
		if (state.hasProperty(BeltBlock.SLOPE) && (state.getValue(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS
			|| state.getValue(BeltBlock.SLOPE) == BeltSlope.VERTICAL))
			return false;
		return getMovementFacing() != side.getOpposite();
	}

	private boolean isOccupied(Direction side) {
		BeltBlockEntity nextBeltController = getControllerBE();
		if (nextBeltController == null)
			return true;
		BeltInventory nextInventory = nextBeltController.getInventory();
		if (nextInventory == null)
			return true;
		if (getSpeed() == 0)
			return true;
		if (getMovementFacing() == side.getOpposite())
			return true;
		if (!nextInventory.canInsertAtFromSide(index, side))
			return true;
		return false;
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
		BeltBlockEntity nextBeltController = getControllerBE();
		ItemStack inserted = transportedStack.stack;
		ItemStack empty = ItemStack.EMPTY;

		if (!BeltBlock.canTransportObjects(getBlockState()))
			return inserted;
		if (nextBeltController == null)
			return inserted;
		BeltInventory nextInventory = nextBeltController.getInventory();
		if (nextInventory == null)
			return inserted;

		BlockEntity teAbove = level.getBlockEntity(worldPosition.above());
		if (teAbove instanceof BrassTunnelBlockEntity tunnelBE) {
			if (tunnelBE.hasDistributionBehaviour()) {
				if (!tunnelBE.getStackToDistribute()
					.isEmpty())
					return inserted;
				if (!tunnelBE.testFlapFilter(side.getOpposite(), inserted))
					return inserted;
				if (!simulate) {
					BeltTunnelInteractionHandler.flapTunnel(nextInventory, index, side.getOpposite(), true);
					tunnelBE.setStackToDistribute(inserted, side.getOpposite());
				}
				return empty;
			}
		}

		if (isOccupied(side))
			return inserted;
		if (simulate)
			return empty;

		transportedStack = transportedStack.copy();
		transportedStack.beltPosition = index + .5f - Math.signum(getDirectionAwareBeltMovementSpeed()) / 16f;

		Direction movementFacing = getMovementFacing();
		if (!side.getAxis()
			.isVertical()) {
			if (movementFacing != side) {
				transportedStack.sideOffset = side.getAxisDirection()
					.getStep() * .675f;
				if (side.getAxis() == Axis.X)
					transportedStack.sideOffset *= -1;
			} else {
				// This creates a smoother transition from belt to belt
				float extraOffset = transportedStack.prevBeltPosition != 0
					&& BeltHelper.getSegmentBE(level, worldPosition.relative(movementFacing.getOpposite())) != null
						? .26f
						: 0;
				transportedStack.beltPosition =
					getDirectionAwareBeltMovementSpeed() > 0 ? index - extraOffset : index + 1 + extraOffset;
			}
		}

		transportedStack.prevSideOffset = transportedStack.sideOffset;
		transportedStack.insertedAt = index;
		transportedStack.insertedFrom = side;
		transportedStack.prevBeltPosition = transportedStack.beltPosition;

		BeltTunnelInteractionHandler.flapTunnel(nextInventory, index, side.getOpposite(), true);

		nextInventory.addItem(transportedStack);
		nextBeltController.setChanged();
		nextBeltController.sendData();
		return empty;
	}

	@Override
	public ModelData getModelData() {
		return ModelData.builder()
			.with(BeltModelData.CASING_PROPERTY, casing)
			.with(BeltModelData.COVER_PROPERTY, covered)
			.build();
	}

	private class BeltSegmentResourceHandler implements ResourceHandler<ItemResource> {
		private final SlotJournal journal = new SlotJournal();

		@Override
		public int size() {
			return 1;
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(itemHandler == null ? ItemStack.EMPTY : itemHandler.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return itemHandler == null ? 0 : itemHandler.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (itemHandler == null)
				return 0;
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return itemHandler.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return itemHandler != null && (resource.isEmpty() || itemHandler.isItemValid(index, resource.toStack(1)));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (itemHandler == null || resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack stack = resource.toStack(amount);
			ItemStack remainder = itemHandler.insertItem(index, stack, true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			itemHandler.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (itemHandler == null || resource.isEmpty() || amount <= 0)
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

		private class SlotJournal extends SnapshotJournal<TransportedItemStack> {
			@Override
			protected TransportedItemStack createSnapshot() {
				BeltInventory beltInventory = getInventory();
				if (beltInventory == null)
					return null;
				TransportedItemStack stack = beltInventory.getStackAtOffset(index);
				return stack == null ? null : stack.copy();
			}

			@Override
			protected void revertToSnapshot(TransportedItemStack snapshot) {
				BeltInventory beltInventory = getInventory();
				if (beltInventory == null)
					return;
				TransportedItemStack current = beltInventory.getStackAtOffset(index);
				if (current != null)
					beltInventory.getTransportedItems()
						.remove(current);
				if (snapshot != null)
					beltInventory.getTransportedItems()
						.add(snapshot.copy());
				setChanged();
				sendData();
			}
		}
	}

	@Override
	protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
		return state.hasProperty(BeltBlock.SLOPE) && (state.getValue(BeltBlock.SLOPE) == BeltSlope.UPWARD
			|| state.getValue(BeltBlock.SLOPE) == BeltSlope.DOWNWARD);
	}

	@Override
	public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
									 boolean connectedViaAxes, boolean connectedViaCogs) {
		if (target instanceof BeltBlockEntity belt && !connectedViaAxes)
			return getController().equals(belt.getController()) ? 1 : 0;
		return 0;
	}

	public void invalidateItemHandler() {
		invalidateCapabilities();
		itemHandler = null;
	}

	public boolean shouldRenderNormally() {
		if (level == null)
			return isController();
		BlockState state = getBlockState();
		return state != null && state.hasProperty(BeltBlock.PART) && state.getValue(BeltBlock.PART) == BeltPart.START;
	}

	public void setCovered(boolean blockCoveringBelt) {
		if (blockCoveringBelt == covered)
			return;
		covered = blockCoveringBelt;
		notifyUpdate();
	}
}
