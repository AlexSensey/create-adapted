package com.simibubi.create.content.kinetics.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.Mode;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.createmod.catnip.api.level.wrapper.SchematicLevel;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.extensions.IBaseRailBlockExtension;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;

public class DeployerMovementBehaviour implements MovementBehaviour {

	@Override
	public Vec3 getActiveAreaOffset(MovementContext context) {
		return Vec3.atLowerCornerOf(context.state.getValue(DeployerBlock.FACING)
			.getUnitVec3i())
			.scale(2);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		if (context.world.isClientSide())
			return;

		tryGrabbingItem(context);
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		Mode mode = getMode(context);
		if (mode == Mode.USE && !DeployerHandler.shouldActivate(player.getMainHandItem(), context.world, pos, null))
			return;

		activate(context, pos, player, mode);
		checkForTrackPlacementAdvancement(context, player);
		tryDisposeOfExcess(context);
		context.stall = player.blockBreakingProgress != null;
	}

	public void activate(MovementContext context, BlockPos pos, DeployerFakePlayer player, Mode mode) {
		Level world = context.world;
		player.placedTracks = false;

		FilterItemStack filter = context.getFilterFromBE();
		if (AllItems.SCHEMATIC.isIn(filter.item())) {
			activateAsSchematicPrinter(context, pos, player, world, filter.item());
			return;
		}

		Vec3 facingVec = Vec3.atLowerCornerOf(context.state.getValue(DeployerBlock.FACING)
			.getUnitVec3i());
		facingVec = context.rotation.apply(facingVec);
		Vec3 vec = context.position.subtract(facingVec.scale(2));

		float xRot = AbstractContraptionEntity.pitchFromVector(facingVec) - 90;
		if (Math.abs(xRot) > 89) {
			Vec3 initial = new Vec3(0, 0, 1);
			if (context.contraption.entity instanceof OrientedContraptionEntity oce)
				initial = VecHelper.rotate(initial, oce.getInitialYaw(), Axis.Y);
			if (context.contraption.entity instanceof CarriageContraptionEntity)
				initial = VecHelper.rotate(initial, 90, Axis.Y);
			facingVec = context.rotation.apply(initial);
		}

		player.setYRot(AbstractContraptionEntity.yawFromVector(facingVec));
		player.setXRot(xRot);
		DeployerHandler.activate(player, vec, pos, facingVec, mode);
	}

	protected void checkForTrackPlacementAdvancement(MovementContext context, DeployerFakePlayer player) {
		if (!(context.contraption instanceof MountedContraption || context.contraption instanceof CarriageContraption)
			|| !player.placedTracks || context.blockEntityData == null)
			return;
		UUID owner = readOwner(context.blockEntityData);
		if (owner != null && context.world.getPlayerByUUID(owner) != null)
			AllAdvancements.SELF_DEPLOYING.awardTo(context.world.getPlayerByUUID(owner));
	}

	protected void activateAsSchematicPrinter(MovementContext context, BlockPos pos, DeployerFakePlayer player,
		Level level, ItemStack filter) {
		if (!filter.has(AllDataComponents.SCHEMATIC_ANCHOR) || !level.getBlockState(pos).canBeReplaced()
			|| !filter.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false))
			return;
		SchematicLevel schematicWorld = SchematicInstances.get(level, filter);
		if (schematicWorld == null || !schematicWorld.getBounds().isInside(pos.subtract(schematicWorld.anchor)))
			return;

		BlockState blockState = schematicWorld.getBlockState(pos);
		ItemRequirement requirement = ItemRequirement.of(blockState, schematicWorld.getBlockEntity(pos));
		if (requirement.isInvalid() || requirement.isEmpty() || AllBlocks.BELT.has(blockState))
			return;

		List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
		ItemStack contextStack = requiredItems.isEmpty() ? ItemStack.EMPTY : requiredItems.get(0).stack;
		if (!context.contraption.hasUniversalCreativeCrate) {
			IItemHandler itemHandler = context.contraption.getStorage().getAllItems();
			for (ItemRequirement.StackRequirement required : requiredItems) {
				ItemStack stack = ItemHelper.extract(itemHandler, required::matches, ExtractionCountMode.EXACTLY,
					required.stack.getCount(), true);
				if (stack.isEmpty())
					return;
			}
			for (ItemRequirement.StackRequirement required : requiredItems)
				contextStack = ItemHelper.extract(itemHandler, required::matches, ExtractionCountMode.EXACTLY,
					required.stack.getCount(), false);
		}

		CompoundTag data = BlockHelper.prepareBlockEntityData(level, blockState, schematicWorld.getBlockEntity(pos));
		BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
		BlockHelper.placeSchematicBlock(level, blockState, pos, contextStack, data);
		if (EventHooks.onBlockPlace(player, snapshot, Direction.UP))
			snapshot.restore(Block.UPDATE_CLIENTS);
		else if (blockState.getBlock() instanceof IBaseRailBlockExtension || blockState.getBlock() instanceof ITrackBlock)
			player.placedTracks = true;
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide() || !context.stall)
			return;
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;

		Pair<BlockPos, Float> progress = player.blockBreakingProgress;
		if (progress != null) {
			int timer = context.data.getIntOr("Timer", 0);
			if (timer < 20) {
				context.data.putInt("Timer", timer + 1);
				return;
			}
			context.data.remove("Timer");
			activate(context, progress.getKey(), player, getMode(context));
			tryDisposeOfExcess(context);
		}
		context.stall = player.blockBreakingProgress != null;
	}

	@Override
	public void cancelStall(MovementContext context) {
		if (context.world.isClientSide())
			return;
		MovementBehaviour.super.cancelStall(context);
		DeployerFakePlayer player = getPlayer(context);
		if (player == null || player.blockBreakingProgress == null)
			return;
		context.world.destroyBlockProgress(player.getId(), player.blockBreakingProgress.getKey(), -1);
		player.blockBreakingProgress = null;
	}

	@Override
	public void stopMoving(MovementContext context) {
		if (context.world.isClientSide())
			return;
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		cancelStall(context);
		saveInventory(context.blockEntityData, player.getInventory(), context.world);
		player.discard();
		context.temporaryData = null;
	}

	private void tryGrabbingItem(MovementContext context) {
		DeployerFakePlayer player = getPlayer(context);
		if (player == null || !player.getMainHandItem().isEmpty())
			return;
		FilterItemStack filter = context.getFilterFromBE();
		if (AllItems.SCHEMATIC.isIn(filter.item()))
			return;
		ItemStack held = ItemHelper.extract(context.contraption.getStorage().getAllItems(),
			stack -> filter.test(context.world, stack), 1, false);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
	}

	private void tryDisposeOfExcess(MovementContext context) {
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		Inventory inventory = player.getInventory();
		FilterItemStack filter = context.getFilterFromBE();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty())
				continue;
			if (slot == inventory.getSelectedSlot() && filter.test(context.world, stack))
				continue;
			collectOrDropItem(context, stack.copy());
			inventory.setItem(slot, ItemStack.EMPTY);
		}
	}

	@Override
	public void writeExtraData(MovementContext context) {
		DeployerFakePlayer player = getPlayer(context);
		if (player != null)
			context.data.put("HeldItem", writeItemStack(player.getMainHandItem(), context.world));
	}

	@Nullable
	static DeployerFakePlayer getPlayer(MovementContext context) {
		if (context.temporaryData instanceof DeployerFakePlayer fakePlayer)
			return fakePlayer;
		if (!(context.world instanceof ServerLevel serverLevel))
			return null;

		DeployerFakePlayer fakePlayer = new DeployerFakePlayer(serverLevel, readOwner(context.blockEntityData));
		fakePlayer.onMinecartContraption = context.contraption instanceof MountedContraption;
		loadInventory(context.blockEntityData.getListOrEmpty("Inventory"), fakePlayer.getInventory(), context.world);
		if (context.data.contains("HeldItem"))
			fakePlayer.setItemInHand(InteractionHand.MAIN_HAND,
				readItemStack(context.data.getCompoundOrEmpty("HeldItem"), context.world));
		context.blockEntityData.remove("Inventory");
		context.temporaryData = fakePlayer;
		return fakePlayer;
	}

	private static Mode getMode(MovementContext context) {
		String value = context.blockEntityData.getStringOr("Mode", Mode.USE.name());
		try {
			return Mode.valueOf(value);
		} catch (IllegalArgumentException ignored) {
			return Mode.USE;
		}
	}

	@Nullable
	private static UUID readOwner(CompoundTag tag) {
		String value = tag.getStringOr("Owner", "");
		if (value.isBlank())
			return null;
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static void loadInventory(ListTag tag, Inventory inventory, Level level) {
		ItemStackWithSlot.CODEC.listOf()
			.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
			.resultOrPartial(error -> Create.LOGGER.error("Failed to load moving deployer inventory: {}", error))
			.ifPresent(entries -> entries.forEach(entry -> {
				if (entry.isValidInContainer(inventory.getContainerSize()))
					inventory.setItem(entry.slot(), entry.stack());
			}));
	}

	private static void saveInventory(CompoundTag tag, Inventory inventory, Level level) {
		List<ItemStackWithSlot> entries = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty())
				entries.add(new ItemStackWithSlot(slot, stack.copy()));
		}
		ItemStackWithSlot.CODEC.listOf()
			.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), entries)
			.resultOrPartial(error -> Create.LOGGER.error("Failed to save moving deployer inventory: {}", error))
			.ifPresent(saved -> tag.put("Inventory", saved));
	}

	static CompoundTag writeItemStack(ItemStack stack, Level level) {
		return ItemStack.OPTIONAL_CODEC
			.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack)
			.resultOrPartial(error -> Create.LOGGER.error("Failed to save moving deployer item: {}", error))
			.filter(CompoundTag.class::isInstance)
			.map(CompoundTag.class::cast)
			.orElseGet(CompoundTag::new);
	}

	static ItemStack readItemStack(CompoundTag tag, Level level) {
		return ItemStack.OPTIONAL_CODEC
			.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
			.resultOrPartial(error -> Create.LOGGER.error("Failed to load moving deployer item: {}", error))
			.orElse(ItemStack.EMPTY);
	}

	@Override
	public boolean disableBlockEntityRendering() {
		// Moving deployers have their pole and hand submitted by the contraption renderer.
		return true;
	}
}
