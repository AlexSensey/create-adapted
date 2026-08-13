package com.simibubi.create.content.fluids.tank;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class CreativeFluidTankBlockEntity extends FluidTankBlockEntity {
	private ResourceHandler<FluidResource> creativeFluidResourceCapability;

	public CreativeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, AllBlockEntityTypes.CREATIVE_FLUID_TANK.get(),
			(be, side) -> be.getFluidResourceCapability());
	}

	@Override
	protected SmartFluidTank createInventory() {
		return new CreativeSmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
	}

	@Override
	protected ResourceHandler<FluidResource> getFluidResourceCapability() {
		if (creativeFluidResourceCapability == null)
			creativeFluidResourceCapability = new CreativeFluidResourceHandler();
		return creativeFluidResourceCapability;
	}

	@Override
	void refreshCapability() {
		super.refreshCapability();
		creativeFluidResourceCapability = null;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return false;
	}

	private CreativeSmartFluidTank creativeTank() {
		return (CreativeSmartFluidTank) tankInventory;
	}

	private CreativeFluidTankBlockEntity getCreativeControllerBE() {
		FluidTankBlockEntity controllerBE = getControllerBE();
		if (controllerBE instanceof CreativeFluidTankBlockEntity creativeController)
			return creativeController;
		return this;
	}

	private class CreativeFluidResourceHandler implements ResourceHandler<FluidResource> {
		private final CreativeFluidJournal journal = new CreativeFluidJournal();

		@Override
		public int size() {
			return 1;
		}

		@Override
		public FluidResource getResource(int index) {
			return FluidResource.of(getCreativeControllerBE().tankInventory.getFluid());
		}

		@Override
		public long getAmountAsLong(int index) {
			return getCreativeControllerBE().tankInventory.getFluidAmount();
		}

		@Override
		public long getCapacityAsLong(int index, FluidResource resource) {
			return getCreativeControllerBE().tankInventory.getCapacity();
		}

		@Override
		public boolean isValid(int index, FluidResource resource) {
			return !resource.isEmpty() && getCreativeControllerBE().tankInventory.isFluidValid(resource.toStack(1));
		}

		@Override
		public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			if (amount <= 0 || !isValid(index, resource))
				return 0;
			journal.updateSnapshots(transaction);
			getCreativeControllerBE().creativeTank()
				.setContainedFluidSilently(resource.toStack(amount));
			return amount;
		}

		@Override
		public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			if (amount <= 0 || resource.isEmpty())
				return 0;
			FluidTankBlockEntity controllerBE = getCreativeControllerBE();
			FluidStack contained = controllerBE.tankInventory.getFluid();
			if (contained.isEmpty() || !resource.matches(contained))
				return 0;
			return Math.min(amount, controllerBE.tankInventory.getFluidAmount());
		}

		private class CreativeFluidJournal extends SnapshotJournal<FluidStack> {
			@Override
			protected FluidStack createSnapshot() {
				return getCreativeControllerBE().tankInventory.getFluid()
					.copy();
			}

			@Override
			protected void revertToSnapshot(FluidStack snapshot) {
				getCreativeControllerBE().creativeTank()
					.setFluidSilently(snapshot);
			}

			@Override
			protected void onRootCommit(FluidStack snapshot) {
				CreativeFluidTankBlockEntity controllerBE = getCreativeControllerBE();
				controllerBE.onFluidStackChanged(controllerBE.tankInventory.getFluid());
			}
		}
	}

	public static class CreativeSmartFluidTank extends SmartFluidTank {
		public static final Codec<CreativeSmartFluidTank> CODEC = RecordCodecBuilder.create(i -> i.group(
			FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(FluidTank::getFluid),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(FluidTank::getCapacity)
		).apply(i, (fluid, capacity) -> {
			CreativeSmartFluidTank tank = new CreativeSmartFluidTank(capacity, $ -> {
			});
			tank.setFluid(fluid);
			return tank;
		}));

		public CreativeSmartFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
			super(capacity, updateCallback);
		}

		@Override
		public int getFluidAmount() {
			return getFluid().isEmpty() ? 0 : getTankCapacity(0);
		}

		public void setContainedFluid(FluidStack fluidStack) {
			fluid = fluidStack.copy();
			if (!fluidStack.isEmpty())
				fluid.setAmount(getTankCapacity(0));
			onContentsChanged();
		}

		public void setContainedFluidSilently(FluidStack fluidStack) {
			fluid = fluidStack.copy();
			if (!fluidStack.isEmpty())
				fluid.setAmount(getTankCapacity(0));
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return resource.getAmount();
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return super.drain(resource, FluidAction.SIMULATE);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return super.drain(maxDrain, FluidAction.SIMULATE);
		}

	}

}
