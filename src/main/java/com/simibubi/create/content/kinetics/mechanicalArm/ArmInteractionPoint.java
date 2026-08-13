package com.simibubi.create.content.kinetics.mechanicalArm;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ArmInteractionPoint {

	protected final ArmInteractionPointType type;
	protected Level level;
	protected final BlockPos pos;
	protected Mode mode = Mode.DEPOSIT;

	protected BlockState cachedState;
	protected BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> cachedHandler;
	protected IItemHandler cachedItemHandler;
	protected ArmAngleTarget cachedAngles;

	public ArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
		this.type = type;
		this.level = level;
		this.pos = pos;
		this.cachedState = state;
	}

	public ArmInteractionPointType getType() {
		return type;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public BlockPos getPos() {
		return pos;
	}

	public Mode getMode() {
		return mode;
	}

	public void cycleMode() {
		mode = mode == Mode.DEPOSIT ? Mode.TAKE : Mode.DEPOSIT;
	}

	protected Vec3 getInteractionPositionVector() {
		return VecHelper.getCenterOf(pos);
	}

	protected Direction getInteractionDirection() {
		return Direction.DOWN;
	}

	public ArmAngleTarget getTargetAngles(BlockPos armPos, boolean ceiling) {
		if (cachedAngles == null)
			cachedAngles =
				new ArmAngleTarget(armPos, getInteractionPositionVector(), getInteractionDirection(), ceiling);

		return cachedAngles;
	}

	public void updateCachedState() {
		BlockState oldState = cachedState;
		cachedState = level.getBlockState(pos);
		if (cachedHandler != null && oldState != cachedState) {
			level.invalidateCapabilities(cachedHandler.pos());
			cachedHandler = null;
			cachedItemHandler = null;
		}
	}

	public boolean isValid() {
		updateCachedState();
		return type.canCreatePoint(level, pos, cachedState);
	}

	public void keepAlive() {
	}

	@Nullable
	protected IItemHandler getHandler(ArmBlockEntity armBlockEntity) {
		if (cachedHandler == null && level instanceof ServerLevel serverLevel) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be == null)
				return null;
			cachedHandler = BlockCapabilityCache.create(
				Capabilities.Item.BLOCK,
				serverLevel,
				pos,
				Direction.UP,
				() -> !armBlockEntity.isRemoved(),
				() -> {
					cachedHandler = null;
					cachedItemHandler = null;
				}
			);
		}
		if (cachedHandler == null)
			return null;
		ResourceHandler<ItemResource> handler = cachedHandler.getCapability();
		if (handler == null) {
			cachedItemHandler = null;
			return null;
		}
		if (cachedItemHandler instanceof ResourceHandlerItemAdapter adapter && adapter.handler == handler)
			return cachedItemHandler;
		return cachedItemHandler = new ResourceHandlerItemAdapter(handler);
	}

	public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
		IItemHandler handler = getHandler(armBlockEntity);
		if (handler == null)
			return stack;
		return ItemHandlerHelper.insertItem(handler, stack, simulate);
	}

	public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
		IItemHandler handler = getHandler(armBlockEntity);
		if (handler == null)
			return ItemStack.EMPTY;
		return handler.extractItem(slot, amount, simulate);
	}

	public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, boolean simulate) {
		return extract(armBlockEntity, slot, 64, simulate);
	}

	public int getSlotCount(ArmBlockEntity armBlockEntity) {
		IItemHandler handler = getHandler(armBlockEntity);
		if (handler == null)
			return 0;
		return handler.getSlots();
	}

	protected void serialize(CompoundTag nbt, BlockPos anchor) {
		nbt.putString("Mode", mode.name());
	}

	protected void deserialize(CompoundTag nbt, BlockPos anchor) {
		mode = Mode.valueOf(nbt.getStringOr("Mode", Mode.DEPOSIT.name()));
	}

	public final CompoundTag serialize(BlockPos anchor) {
		Identifier key = CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE.getKey(type);
		if (key == null)
			throw new IllegalArgumentException("Could not get id for ArmInteractionPointType " + type + "!");

		CompoundTag nbt = new CompoundTag();
		nbt.putString("Type", key.toString());
		nbt.put("Pos", writeBlockPos(pos.subtract(anchor)));
		serialize(nbt, anchor);
		return nbt;
	}

	@Nullable
	public static ArmInteractionPoint deserialize(CompoundTag nbt, Level level, BlockPos anchor) {
		Identifier id = Identifier.tryParse(nbt.getStringOr("Type", ""));
		if (id == null)
			return null;
		ArmInteractionPointType type = CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE.getValue(id);
		if (type == null)
			return null;
		BlockPos pos = readBlockPos(nbt.getCompoundOrEmpty("Pos")).offset(anchor);
		BlockState state = level.getBlockState(pos);
		if (!type.canCreatePoint(level, pos, state))
			return null;
		ArmInteractionPoint point = type.createPoint(level, pos, state);
		if (point == null)
			return null;
		point.deserialize(nbt, anchor);
		return point;
	}

	public static void transformPos(CompoundTag nbt, StructureTransform transform) {
		BlockPos pos = readBlockPos(nbt.getCompoundOrEmpty("Pos"));
		pos = transform.applyWithoutOffset(pos);
		nbt.put("Pos", writeBlockPos(pos));
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

	public static boolean isInteractable(Level level, BlockPos pos, BlockState state) {
		return ArmInteractionPointType.getPrimaryType(level, pos, state) != null;
	}

	@Nullable
	public static ArmInteractionPoint create(Level level, BlockPos pos, BlockState state) {
		ArmInteractionPointType type = ArmInteractionPointType.getPrimaryType(level, pos, state);
		if (type == null)
			return null;
		return type.createPoint(level, pos, state);
	}

	public enum Mode {
		DEPOSIT("mechanical_arm.deposit_to", 0xDDC166),
		TAKE("mechanical_arm.extract_from", 0x7FCDE0);

		private final String translationKey;
		private final int color;

		Mode(String translationKey, int color) {
			this.translationKey = translationKey;
			this.color = color;
		}

		public String getTranslationKey() {
			return translationKey;
		}

		public int getColor() {
			return color;
		}
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
			return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate, null);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0)
				return ItemStack.EMPTY;
			ItemResource resource = handler.getResource(slot);
			if (resource.isEmpty())
				return ItemStack.EMPTY;
			amount = Math.min(amount, resource.getMaxStackSize());
			try (Transaction transaction = Transaction.openRoot()) {
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
