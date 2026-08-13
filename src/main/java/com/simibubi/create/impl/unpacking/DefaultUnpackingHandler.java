package com.simibubi.create.impl.unpacking;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public enum DefaultUnpackingHandler implements UnpackingHandler {
	INSTANCE;

	@Override
	public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
		ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, pos, side);
		if (target == null)
			return false;

		try (Transaction transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			for (ItemStack stack : items) {
				if (stack.isEmpty())
					continue;

				ItemResource resource = ItemResource.of(stack);
				int remaining = stack.getCount();
				for (int slot = 0; slot < target.size() && remaining > 0; slot++)
					remaining -= target.insert(slot, resource, remaining, transaction);

				if (remaining > 0)
					return false;
			}

			if (!simulate)
				transaction.commit();
			return true;
		}
	}
}
