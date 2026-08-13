package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.google.common.base.Predicates;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.createmod.catnip.api.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TankManipulationBehaviour extends CapManipulationBehaviourBase<IFluidHandler, TankManipulationBehaviour> {

	public static final BehaviourType<TankManipulationBehaviour> OBSERVE = new BehaviourType<>();
	private BehaviourType<TankManipulationBehaviour> behaviourType;

	public TankManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
		this(OBSERVE, be, target);
	}

	private TankManipulationBehaviour(BehaviourType<TankManipulationBehaviour> type, SmartBlockEntity be,
		InterfaceProvider target) {
		super(be, target);
		behaviourType = type;
	}

	public FluidStack extractAny() {
		if (!hasInventory())
			return FluidStack.EMPTY;
		IFluidHandler inventory = getInventory();
		Predicate<FluidStack> filterTest = getFilterTest(Predicates.alwaysTrue());
		for (int i = 0; i < inventory.getTanks(); i++) {
			FluidStack fluidInTank = inventory.getFluidInTank(i);
			if (fluidInTank.isEmpty())
				continue;
			if (!filterTest.test(fluidInTank))
				continue;
			FluidStack drained =
				inventory.drain(fluidInTank, simulateNext ? FluidAction.SIMULATE : FluidAction.EXECUTE);
			if (!drained.isEmpty())
				return drained;
		}

		return FluidStack.EMPTY;
	}

	protected Predicate<FluidStack> getFilterTest(Predicate<FluidStack> customFilter) {
		Predicate<FluidStack> test = customFilter;
		FilteringBehaviour filter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
		if (filter != null)
			test = customFilter.and(filter::test);
		return test;
	}

	@Override
	public BehaviourType<?> getType() {
		return behaviourType;
	}

	@Override
	@Nullable
	protected net.neoforged.neoforge.capabilities.BlockCapability<IFluidHandler, Direction> capability() {
		return null;
	}

	@Override
	public void findNewCapability() {
		Level world = getWorld();
		BlockFace targetBlockFace = getTarget().getOpposite();
		BlockPos pos = targetBlockFace.getPos();

		targetCapability = null;

		if (!world.isLoaded(pos))
			return;
		BlockEntity invBE = world.getBlockEntity(pos);
		if (!filter.test(invBE))
			return;
		ResourceHandler<FluidResource> handler =
			world.getCapability(Capabilities.Fluid.BLOCK, pos, bypassSided ? null : targetBlockFace.getFace());
		if (handler == null)
			return;
		targetCapability = new ResourceHandlerFluidAdapter(handler);
	}

	private static class ResourceHandlerFluidAdapter implements IFluidHandler {
		private final ResourceHandler<FluidResource> handler;

		private ResourceHandlerFluidAdapter(ResourceHandler<FluidResource> handler) {
			this.handler = handler;
		}

		@Override
		public int getTanks() {
			return handler.size();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return FluidUtil.getStack(handler, tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return handler.getCapacityAsInt(tank, FluidResource.EMPTY);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return handler.isValid(tank, FluidResource.of(stack));
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return 0;
			try (Transaction transaction = Transaction.openRoot()) {
				int inserted = handler.insert(FluidResource.of(resource), resource.getAmount(), transaction);
				if (action.execute())
					transaction.commit();
				return inserted;
			}
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return FluidStack.EMPTY;
			try (Transaction transaction = Transaction.openRoot()) {
				int extracted = handler.extract(FluidResource.of(resource), resource.getAmount(), transaction);
				if (action.execute())
					transaction.commit();
				return extracted == 0 ? FluidStack.EMPTY : resource.copyWithAmount(extracted);
			}
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (maxDrain <= 0)
				return FluidStack.EMPTY;
			try (Transaction transaction = Transaction.openRoot()) {
				var extracted = ResourceHandlerUtil.extractFirst(handler, $ -> true, maxDrain, transaction);
				if (action.execute())
					transaction.commit();
				return extracted == null ? FluidStack.EMPTY : extracted.resource().toStack(extracted.amount());
			}
		}
	}

}
