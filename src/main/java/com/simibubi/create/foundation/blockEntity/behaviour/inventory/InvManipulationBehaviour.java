package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;

import net.createmod.catnip.api.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class InvManipulationBehaviour extends CapManipulationBehaviourBase<IItemHandler, InvManipulationBehaviour> {

	// Extra types available for multibehaviour
	public static final BehaviourType<InvManipulationBehaviour>

	TYPE = new BehaviourType<>(), EXTRACT = new BehaviourType<>(), INSERT = new BehaviourType<>();

	private BehaviourType<InvManipulationBehaviour> behaviourType;

	public static InvManipulationBehaviour forExtraction(SmartBlockEntity be, InterfaceProvider target) {
		return new InvManipulationBehaviour(EXTRACT, be, target);
	}

	public static InvManipulationBehaviour forInsertion(SmartBlockEntity be, InterfaceProvider target) {
		return new InvManipulationBehaviour(INSERT, be, target);
	}

	public InvManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
		this(TYPE, be, target);
	}

	private InvManipulationBehaviour(BehaviourType<InvManipulationBehaviour> type, SmartBlockEntity be,
		InterfaceProvider target) {
		super(be, target);
		behaviourType = type;
	}

	@Nullable
	public IdentifiedInventory getIdentifiedInventory() {
		IItemHandler inventory = this.getInventory();
		if (inventory == null)
			return null;

		InventoryIdentifier identifier = InventoryIdentifier.get(this.getWorld(), this.getTarget().getOpposite());
		return new IdentifiedInventory(identifier, inventory);
	}

	@Override
	protected BlockCapability<IItemHandler, Direction> capability() {
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
		ResourceHandler<ItemResource> handler =
			world.getCapability(Capabilities.Item.BLOCK, pos, bypassSided ? null : targetBlockFace.getFace());
		if (handler == null)
			return;
		targetCapability = new ResourceHandlerItemAdapter(handler);
	}

	public ItemStack extract() {
		return extract(getModeFromFilter(), getAmountFromFilter());
	}

	public ItemStack extract(ExtractionCountMode mode, int amount) {
		return extract(mode, amount, Predicates.alwaysTrue());
	}

	public ItemStack extract(ExtractionCountMode mode, int amount, Predicate<ItemStack> filter) {
		boolean shouldSimulate = simulateNext;
		simulateNext = false;

		if (getWorld().isClientSide())
			return ItemStack.EMPTY;
		IItemHandler inventory = targetCapability;
		if (inventory == null)
			return ItemStack.EMPTY;

		Predicate<ItemStack> test = getFilterTest(filter);
		return ItemHelper.extract(inventory, test, mode, amount, shouldSimulate);
	}

	public ItemStack insert(ItemStack stack) {
		boolean shouldSimulate = simulateNext;
		simulateNext = false;
		IItemHandler inventory = targetCapability;
		if (inventory == null)
			return stack;
		return ItemHandlerHelper.insertItemStacked(inventory, stack, shouldSimulate);
	}

	protected Predicate<ItemStack> getFilterTest(Predicate<ItemStack> customFilter) {
		Predicate<ItemStack> test = customFilter;
		FilteringBehaviour filter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
		if (filter != null)
			test = customFilter.and(filter::test);
		return test;
	}

	@Override
	public BehaviourType<?> getType() {
		return behaviourType;
	}

	private static class ResourceHandlerItemAdapter implements IItemHandler {
		private final ResourceHandler<ItemResource> handler;

		private ResourceHandlerItemAdapter(ResourceHandler<ItemResource> handler) {
			this.handler = handler;
		}

		@Override
		public int getSlots() {
			return handler.size();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemUtil.getStack(handler, slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			// Item handlers can be invoked from inside another ResourceHandler operation
			// (for example, a funnel inserting a package into a Repackager). Passing null
			// would ask ItemUtil to open a second root transaction, which NeoForge rejects.
			// Reuse the active transaction as the parent when one exists; open(null) still
			// creates the normal root transaction for standalone item-handler calls.
			return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate,
				Transaction.getCurrentOpenedTransaction());
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0)
				return ItemStack.EMPTY;
			ItemResource resource = handler.getResource(slot);
			if (resource.isEmpty())
				return ItemStack.EMPTY;
			amount = Math.min(amount, resource.getMaxStackSize());
			try (Transaction transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				int extracted = handler.extract(slot, resource, amount, transaction);
				if (!simulate)
					transaction.commit();
				return resource.toStack(extracted);
			}
		}

		@Override
		public int getSlotLimit(int slot) {
			return handler.getCapacityAsInt(slot, ItemResource.EMPTY);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return handler.isValid(slot, ItemResource.of(stack));
		}
	}

}
