package com.simibubi.create.content.kinetics.millstone;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class MillstoneBlockEntity extends KineticBlockEntity implements Clearable {
	public ItemStackHandler inputInv;
	public ItemStackHandler outputInv;
	public IItemHandler capability;
	private ResourceHandler<ItemResource> itemResourceCapability;
	public int timer;
	private MillingRecipe lastRecipe;

	public MillstoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inputInv = new ItemStackHandler(1);
		outputInv = new ItemStackHandler(9);
		capability = new MillstoneInventoryHandler();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
			Capabilities.Item.BLOCK,
			AllBlockEntityTypes.MILLSTONE.get(),
			(be, context) -> be.getItemResourceCapability()
		);
	}

	private ResourceHandler<ItemResource> getItemResourceCapability() {
		if (itemResourceCapability == null)
			itemResourceCapability = new MillstoneResourceHandler();
		return itemResourceCapability;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this));
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.MILLSTONE);
	}

	@Override
	public void tickAudio() {
		super.tickAudio();

		if (getSpeed() == 0)
			return;
		if (inputInv.getStackInSlot(0)
			.isEmpty())
			return;

		float pitch = Mth.clamp((Math.abs(getSpeed()) / 256f) + .45f, .85f, 1f);
		SoundScapes.play(AmbienceGroup.MILLING, worldPosition, pitch);
	}

	@Override
	public void tick() {
		super.tick();

		if (getSpeed() == 0)
			return;
		for (int i = 0; i < outputInv.getSlots(); i++)
			if (outputInv.getStackInSlot(i)
				.getCount() == outputInv.getSlotLimit(i))
				return;

		if (timer > 0) {
			timer -= getProcessingSpeed();

			if (level.isClientSide()) {
				spawnParticles();
				return;
			}
			if (timer <= 0)
				process();
			return;
		}

		if (inputInv.getStackInSlot(0)
			.isEmpty())
			return;

		RecipeWrapper inventoryIn = new RecipeWrapper(inputInv);
		if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
			Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, level);
			if (!recipe.isPresent()) {
				timer = 100;
				sendData();
			} else {
				lastRecipe = recipe.get().value();
				timer = lastRecipe.getProcessingDuration();
				sendData();
			}
			return;
		}

		timer = lastRecipe.getProcessingDuration();
		sendData();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		itemResourceCapability = null;
		invalidateCapabilities();
	}

	@Override
	public void clearContent() {
		((ItemStackHandlerAccessor) inputInv).create$getStacks().clear();
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, inputInv);
		ItemHelper.dropContents(level, worldPosition, outputInv);
	}

	private void process() {
		RecipeWrapper inventoryIn = new RecipeWrapper(inputInv);

		if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
			Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, level);
			if (recipe.isEmpty())
				return;
			lastRecipe = recipe.get().value();
		}

		ItemStack stackInSlot = inputInv.getStackInSlot(0);
		ItemStack craftingRemainingItem = getCraftingRemainder(stackInSlot);
		stackInSlot.shrink(1);
		inputInv.setStackInSlot(0, stackInSlot);
		lastRecipe.rollResults(level.getRandom())
			.forEach(stack -> ItemHandlerHelper.insertItemStacked(outputInv, stack, false));
		if (!craftingRemainingItem.isEmpty()) {
			ItemHandlerHelper.insertItemStacked(outputInv, craftingRemainingItem, false);
		}
		award(AllAdvancements.MILLSTONE);

		sendData();
		setChanged();
	}

	private ItemStack getCraftingRemainder(ItemStack stack) {
		ItemStackTemplate remainder = stack.getItem()
			.getCraftingRemainder();
		return remainder == null ? ItemStack.EMPTY : remainder.create();
	}

	public void spawnParticles() {
		ItemStack stackInSlot = inputInv.getStackInSlot(0);
		if (stackInSlot.isEmpty())
			return;

		ItemParticleOption data = new ItemParticleOption(ParticleTypes.ITEM, stackInSlot.getItem());
		float angle = level.getRandom().nextFloat() * 360;
		Vec3 offset = new Vec3(0, 0, 0.5f);
		offset = VecHelper.rotate(offset, angle, Axis.Y);
		Vec3 target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y);

		Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
		target = VecHelper.offsetRandomly(target.subtract(offset), level.getRandom(), 1 / 128f);
		level.addParticle(data, center.x, center.y, center.z, target.x, target.y, target.z);
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("Timer", timer);
		compound.put("InputInventory", serializeInventory(inputInv, registries));
		compound.put("OutputInventory", serializeInventory(outputInv, registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		timer = compound.getIntOr("Timer", 0);
		deserializeInventory(inputInv, compound.getCompoundOrEmpty("InputInventory"), registries);
		deserializeInventory(outputInv, compound.getCompoundOrEmpty("OutputInventory"), registries);
		super.read(compound, registries, clientPacket);
	}

	private static CompoundTag serializeInventory(ItemStackHandler inventory, HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		ListTag items = new ListTag();
		tag.putInt("Size", inventory.getSlots());

		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;

			CompoundTag itemTag = new CompoundTag();
			itemTag.putInt("Slot", slot);
			ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
				.result()
				.ifPresent(stackTag -> itemTag.put("Stack", stackTag));
			if (itemTag.contains("Stack"))
				items.add(itemTag);
		}

		tag.put("Items", items);
		return tag;
	}

	private static void deserializeInventory(ItemStackHandler inventory, CompoundTag tag, HolderLookup.Provider registries) {
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			inventory.setStackInSlot(slot, ItemStack.EMPTY);

		ListTag items = tag.getListOrEmpty("Items");
		for (int i = 0; i < items.size(); i++) {
			CompoundTag itemTag = items.getCompoundOrEmpty(i);
			int slot = itemTag.getIntOr("Slot", -1);
			if (slot < 0 || slot >= inventory.getSlots())
				continue;

			Tag stackTag = itemTag.get("Stack");
			if (stackTag == null)
				continue;

			ItemStack stack = ItemStack.OPTIONAL_CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), stackTag)
				.result()
				.map(result -> result.getFirst())
				.orElse(ItemStack.EMPTY);
			inventory.setStackInSlot(slot, stack);
		}
	}

	public int getProcessingSpeed() {
		return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
	}

	private boolean canProcess(ItemStack stack) {
		ItemStackHandler tester = new ItemStackHandler(1);
		tester.setStackInSlot(0, stack);
		RecipeWrapper inventoryIn = new RecipeWrapper(tester);

		if (lastRecipe != null && lastRecipe.matches(inventoryIn, level))
			return true;
		return AllRecipeTypes.MILLING.find(inventoryIn, level)
			.isPresent();
	}

	private class MillstoneInventoryHandler extends CombinedInvWrapper {

		public MillstoneInventoryHandler() {
			super(inputInv, outputInv);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)))
				return false;
			return canProcess(stack) && super.isItemValid(slot, stack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)))
				return stack;
			if (!isItemValid(slot, stack))
				return stack;
			ItemStack remainder = super.insertItem(slot, stack, simulate);
			if (!simulate && remainder.getCount() != stack.getCount())
				sendData();
			return remainder;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (inputInv == getHandlerFromIndex(getIndexForSlot(slot)))
				return ItemStack.EMPTY;
			ItemStack extracted = super.extractItem(slot, amount, simulate);
			if (!simulate && !extracted.isEmpty())
				sendData();
			return extracted;
		}

	}

	private class MillstoneResourceHandler implements ResourceHandler<ItemResource> {
		private final SnapshotJournal<MillstoneSnapshot> journal = new SnapshotJournal<>() {
			@Override
			protected MillstoneSnapshot createSnapshot() {
				List<ItemStack> input = snapshot(inputInv);
				List<ItemStack> output = snapshot(outputInv);
				return new MillstoneSnapshot(input, output, timer, lastRecipe);
			}

			@Override
			protected void revertToSnapshot(MillstoneSnapshot snapshot) {
				restore(inputInv, snapshot.input);
				restore(outputInv, snapshot.output);
				timer = snapshot.timer;
				lastRecipe = snapshot.lastRecipe;
			}
		};

		@Override
		public int size() {
			return capability.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(capability.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return capability.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return capability.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (resource.isEmpty())
				return true;
			return capability.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack remainder = capability.insertItem(index, resource.toStack(amount), true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			capability.insertItem(index, resource.toStack(inserted), false);
			sendData();
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = capability.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = capability.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			capability.extractItem(index, extracted.getCount(), false);
			sendData();
			return extracted.getCount();
		}

		private List<ItemStack> snapshot(ItemStackHandler handler) {
			List<ItemStack> stacks = new java.util.ArrayList<>();
			for (int slot = 0; slot < handler.getSlots(); slot++)
				stacks.add(handler.getStackInSlot(slot)
					.copy());
			return stacks;
		}

		private void restore(ItemStackHandler handler, List<ItemStack> stacks) {
			for (int slot = 0; slot < stacks.size(); slot++)
				handler.setStackInSlot(slot, stacks.get(slot)
					.copy());
		}
	}

	private record MillstoneSnapshot(List<ItemStack> input, List<ItemStack> output, int timer, MillingRecipe lastRecipe) {
	}
}
