package com.simibubi.create.content.fluids.hosePulley;

import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class HosePulleyBlockEntity extends KineticBlockEntity {

	LerpedFloat offset;
	boolean isMoving;

	private SmartFluidTank internalTank;
	private FluidDrainingBehaviour drainer;
	private FluidFillingBehaviour filler;
	private HosePulleyFluidHandler handler;
	private ResourceHandler<FluidResource> fluidResourceCapability;
	private boolean infinite;

	public HosePulleyBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		offset = LerpedFloat.linear()
			.startWithValue(0);
		isMoving = true;
		internalTank = new SmartFluidTank(1500, this::onTankContentsChanged);
		handler = new HosePulleyFluidHandler(internalTank, filler, drainer,
			() -> worldPosition.below((int) Math.ceil(offset.getValue())), () -> !this.isMoving);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, AllBlockEntityTypes.HOSE_PULLEY.get(),
			(be, side) -> {
				if (side == null || HosePulleyBlock.hasPipeTowards(be.level, be.worldPosition, be.getBlockState(), side))
					return be.getFluidResourceCapability();
				return null;
			});
	}

	protected ResourceHandler<FluidResource> getFluidResourceCapability() {
		if (handler == null)
			return null;
		if (fluidResourceCapability == null)
			fluidResourceCapability = new HosePulleyFluidResourceHandler();
		return fluidResourceCapability;
	}

	public IFluidHandler getFluidHandlerForPipe() {
		return handler;
	}

	@Override
	public void sendData() {
		infinite = filler.isInfinite() || drainer.isInfinite();
		super.sendData();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (infinite)
			TooltipHelper.addHint(tooltip, "hint.hose_pulley");
		return addToGoggleTooltip;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		drainer = new FluidDrainingBehaviour(this);
		filler = new FluidFillingBehaviour(this);
		behaviours.add(drainer);
		behaviours.add(filler);
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.HOSE_PULLEY, AllAdvancements.HOSE_PULLEY_LAVA);
	}

	protected void onTankContentsChanged(FluidStack contents) {}

	private void resetFluidManipulation() {
		if (level != null && level.isClientSide())
			return;
		if (drainer != null)
			drainer.reset();
		if (filler != null)
			filler.reset();
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		isMoving = true;
		if (getSpeed() == 0) {
			offset.forceNextSync();
			offset.setValue(Math.round(offset.getValue()));
			isMoving = false;
		}

		if (isMoving) {
			float newOffset = offset.getValue() + getMovementSpeed();
			if (newOffset < 0)
				isMoving = false;
			if (!level.getBlockState(worldPosition.below((int) Math.ceil(newOffset)))
				.canBeReplaced()) {
				isMoving = false;
			}
			if (isMoving) {
				drainer.reset();
				filler.reset();
			}
		}

		super.onSpeedChanged(previousSpeed);
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().expandTowards(0, -offset.getValue(), 0);
	}

	@Override
	public void tick() {
		super.tick();
		boolean wasMoving = isMoving;
		float newOffset = offset.getValue() + getMovementSpeed();
		if (newOffset < 0) {
			newOffset = 0;
			isMoving = false;
		}
		if (!level.getBlockState(worldPosition.below((int) Math.ceil(newOffset)))
			.canBeReplaced()) {
			newOffset = (float) Math.max(0, Math.ceil(newOffset) - 1);
			isMoving = false;
		}
		if (getSpeed() == 0)
			isMoving = false;

		offset.setValue(newOffset);
		if (wasMoving != isMoving)
			resetFluidManipulation();
		invalidateRenderBoundingBox();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide())
			return;
		if (isMoving)
			return;

		int ceil = (int) Math.ceil(offset.getValue() + getMovementSpeed());
		if (getMovementSpeed() > 0 && level.getBlockState(worldPosition.below(ceil))
			.canBeReplaced()) {
			isMoving = true;
			drainer.reset();
			filler.reset();
			return;
		}

		sendData();
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (clientPacket)
			offset.forceNextSync();
		compound.put("Offset", offset.writeNBT());
		writeFluidStack(registries, compound);
		super.write(compound, registries, clientPacket);
		if (clientPacket)
			compound.putBoolean("Infinite", infinite);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		offset.readNBT(compound.getCompoundOrEmpty("Offset"), clientPacket);

		readFluidStack(registries, compound);
		super.read(compound, registries, clientPacket);
		if (clientPacket)
			infinite = compound.getBooleanOr("Infinite", false);
	}

	private void writeFluidStack(HolderLookup.Provider registries, CompoundTag compound) {
		if (internalTank.isEmpty())
			return;

		FluidStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
				internalTank.getFluid())
			.resultOrPartial(error -> Create.LOGGER.error("Failed to serialize hose pulley fluid: {}", error))
			.ifPresent(tag -> compound.put("Tank", tag));
	}

	private void readFluidStack(HolderLookup.Provider registries, CompoundTag compound) {
		if (!compound.contains("Tank")) {
			internalTank.setFluid(FluidStack.EMPTY);
			return;
		}

		Tag tag = compound.get("Tank");
		if (tag == null) {
			internalTank.setFluid(FluidStack.EMPTY);
			return;
		}

		FluidStack.OPTIONAL_CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
			.resultOrPartial(error -> Create.LOGGER.error("Failed to deserialize hose pulley fluid: {}", error))
			.ifPresent(result -> internalTank.setFluid(result.getFirst()));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
		fluidResourceCapability = null;
	}

	public float getMovementSpeed() {
		float movementSpeed = convertToLinear(getSpeed());
		if (level.isClientSide())
			movementSpeed *= ServerSpeedProvider.get();
		return movementSpeed;
	}

	public float getInterpolatedOffset(float pt) {
		return Math.max(offset.getValue(pt), 3 / 16f);
	}

	private class HosePulleyFluidResourceHandler implements ResourceHandler<FluidResource> {
		private final FluidJournal journal = new FluidJournal();

		@Override
		public int size() {
			return handler.getTanks();
		}

		@Override
		public FluidResource getResource(int index) {
			return FluidResource.of(handler.getFluidInTank(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return handler.getFluidInTank(index)
				.getAmount();
		}

		@Override
		public long getCapacityAsLong(int index, FluidResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return handler.getTankCapacity(index);
		}

		@Override
		public boolean isValid(int index, FluidResource resource) {
			if (resource.isEmpty())
				return true;
			return handler.isFluidValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			FluidStack stack = resource.toStack(amount);
			int inserted = handler.fill(stack, FluidAction.SIMULATE);
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			return handler.fill(resource.toStack(inserted), FluidAction.EXECUTE);
		}

		@Override
		public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			FluidStack extracted = handler.drain(resource.toStack(amount), FluidAction.SIMULATE);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			return handler.drain(extracted, FluidAction.EXECUTE)
				.getAmount();
		}

		private class FluidJournal extends SnapshotJournal<FluidStack> {
			@Override
			protected FluidStack createSnapshot() {
				return internalTank.getFluid()
					.copy();
			}

			@Override
			protected void revertToSnapshot(FluidStack snapshot) {
				internalTank.setFluid(snapshot.copy());
			}
		}
	}
}
