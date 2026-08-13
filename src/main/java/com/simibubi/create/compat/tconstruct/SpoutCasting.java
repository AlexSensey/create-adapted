package com.simibubi.create.compat.tconstruct;

import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.ResourceHandlerFluidAdapter;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public enum SpoutCasting implements BlockSpoutingBehaviour {
	INSTANCE;

	@Override
	public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid, boolean simulate) {
		if (!enabled())
			return 0;
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity == null)
			return 0;

		ResourceHandler<FluidResource> resourceHandler =
			level.getCapability(Capabilities.Fluid.BLOCK, blockEntity.getBlockPos(), Direction.UP);
		if (resourceHandler == null)
			return 0;
		IFluidHandler handler = new ResourceHandlerFluidAdapter(resourceHandler);
		if (handler.getTanks() != 1 || !handler.isFluidValid(0, availableFluid))
			return 0;

		FluidStack containedFluid = handler.getFluidInTank(0);
		if (!(containedFluid.isEmpty() || FluidStack.isSameFluidSameComponents(containedFluid, availableFluid)))
			return 0;

		int amount = availableFluid.getAmount();
		if (amount < 1000
			&& handler.fill(FluidHelper.copyStackWithAmount(availableFluid, amount + 1), FluidAction.SIMULATE) > amount)
			return 0;
		return handler.fill(availableFluid, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
	}

	private boolean enabled() {
		return AllConfigs.server().recipes.allowCastingBySpout.get();
	}
}
