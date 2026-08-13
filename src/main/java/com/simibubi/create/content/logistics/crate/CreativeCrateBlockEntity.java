package com.simibubi.create.content.logistics.crate;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class CreativeCrateBlockEntity extends CrateBlockEntity implements Clearable {
	FilteringBehaviour filtering;
	BottomlessItemHandler inv;
	private ResourceHandler<ItemResource> itemResourceCapability;

	public CreativeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inv = new BottomlessItemHandler(() -> filtering == null ? ItemStack.EMPTY : filtering.getFilter());
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.CREATIVE_CRATE.get(),
			(be, side) -> be.getItemResourceCapability());
	}

	private ResourceHandler<ItemResource> getItemResourceCapability() {
		if (itemResourceCapability == null)
			itemResourceCapability = new BottomlessResourceHandler();
		return itemResourceCapability;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = createFilter());
		filtering.setLabel(CreateLang.translateDirect("logistics.creative_crate.supply"));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (inv != null) {
			itemResourceCapability = null;
			invalidateCapabilities();
		}
	}

	@Override
	public void clearContent() {
		filtering.setFilter(ItemStack.EMPTY);
	}

	public FilteringBehaviour createFilter() {
		return new FilteringBehaviour(this, new ValueBoxTransform() {
			@Override
			public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
				TransformStack.of(ms)
					.rotateXDegrees(90);
			}

			@Override
			public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
				return new Vec3(0.5, 13.5 / 16d, 0.5);
			}

			public float getScale() {
				return super.getScale();
			};
		});
	}

	private class BottomlessResourceHandler implements ResourceHandler<ItemResource> {
		@Override
		public int size() {
			return inv.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			if (index < 0 || index >= size())
				return ItemResource.EMPTY;
			return ItemResource.of(inv.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			if (index < 0 || index >= size())
				return 0;
			return inv.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (index < 0 || index >= size())
				return 0;
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return inv.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return index >= 0 && index < size() && (resource.isEmpty() || inv.isItemValid(index, resource.toStack(1)));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (index < 0 || index >= size() || resource.isEmpty() || amount <= 0)
				return 0;
			ItemStack remainder = inv.insertItem(index, resource.toStack(amount), true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;
			inv.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (index < 0 || index >= size() || resource.isEmpty() || amount <= 0)
				return 0;
			ItemStack current = inv.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;
			ItemStack extracted = inv.extractItem(index, amount, true);
			return extracted.getCount();
		}
	}
}
