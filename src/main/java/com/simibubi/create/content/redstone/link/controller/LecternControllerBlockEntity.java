package com.simibubi.create.content.redstone.link.controller;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public class LecternControllerBlockEntity extends SmartBlockEntity {
	private ItemContainerContents controllerData = ItemContainerContents.EMPTY;
	private UUID user;
	private UUID prevUser;    // used only on client
	private boolean deactivatedThisTick;    // used only on server
	private boolean controllerRemoved;

	public LecternControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
		if (user != null)
			compound.putString("User", user.toString());
	}

	@Override
	public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
		super.writeSafe(compound, registries);
		compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);

		controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, registries, compound.get("ControllerData"))
			.orElse(ItemContainerContents.EMPTY);
		user = readUuid(compound, "User");
	}

	private static UUID readUuid(CompoundTag tag, String key) {
		if (!tag.contains(key))
			return null;
		try {
			return UUID.fromString(tag.getStringOr(key, ""));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public ItemStack getController() {
		return createLinkedController();
	}

	public boolean hasUser() {
		return user != null;
	}

	public boolean isUsedBy(Player player) {
		return hasUser() && user.equals(player.getUUID());
	}

	public void tryStartUsing(Player player) {
		if (!deactivatedThisTick && !hasUser())
			startUsing(player);
	}

	public void tryStopUsing(Player player) {
		if (isUsedBy(player))
			stopUsing(player);
	}

	private void startUsing(Player player) {
		user = player.getUUID();
		// Remove the legacy marker. It could survive a disconnect or an interrupted
		// client session and permanently prevent this player from using a controller.
		player.getPersistentData().remove("IsUsingLecternController");
		sendData();
	}

	private void stopUsing(Player player) {
		user = null;
		if (player != null)
			player.getPersistentData().remove("IsUsingLecternController");
		deactivatedThisTick = true;
		sendData();
	}

	@Override
	public void tick() {
		super.tick();

		if (level.isClientSide()) {
			CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tryToggleActive);
			prevUser = user;
		}

		if (!level.isClientSide()) {
			deactivatedThisTick = false;

			if (!(level instanceof ServerLevel))
				return;
			if (user == null)
				return;

			Entity entity = ((ServerLevel) level).getEntity(user);
			if (!(entity instanceof Player player)) {
				stopUsing(null);
				return;
			}

			if (!playerInRange(player, level, worldPosition))
				stopUsing(player);
		}
	}

	private void tryToggleActive() {
		if (user == null && Minecraft.getInstance().player.getUUID().equals(prevUser)) {
			LinkedControllerClientHandler.deactivateInLectern();
		} else if (prevUser == null && Minecraft.getInstance().player.getUUID().equals(user)) {
			LinkedControllerClientHandler.activateInLectern(worldPosition);
		}
	}

	public void setController(ItemStack newController) {
		if (newController != null) {
			controllerRemoved = false;
			controllerData = newController.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
			AllSoundEvents.CONTROLLER_PUT.playOnServer(level, worldPosition);
		}
	}

	public void swapControllers(ItemStack stack, Player player, InteractionHand hand, BlockState state) {
		ItemStack newController = stack.copy();
		stack.setCount(0);
		if (player.getItemInHand(hand).isEmpty()) {
			player.setItemInHand(hand, createLinkedController());
		} else {
			dropController(state);
		}
		setController(newController);
	}

	public void dropController(BlockState state) {
		if (controllerRemoved)
			return;
		controllerRemoved = true;

		Entity entity = ((ServerLevel) level).getEntity(user);
		if (entity instanceof Player player)
			stopUsing(player);

		Direction dir = state.getValue(LecternControllerBlock.FACING);
		double x = worldPosition.getX() + 0.5 + 0.25 * dir.getStepX();
		double y = worldPosition.getY() + 1;
		double z = worldPosition.getZ() + 0.5 + 0.25 * dir.getStepZ();
		ItemEntity itementity = new ItemEntity(level, x, y, z, createLinkedController());
		itementity.setDefaultPickUpDelay();
		level.addFreshEntity(itementity);
		controllerData = ItemContainerContents.EMPTY;
	}

	public static boolean playerInRange(Player player, Level world, BlockPos pos) {
		double reach = player.blockInteractionRange() + .75;
		return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach;
	}

	private ItemStack createLinkedController() {
		ItemStack stack = AllItems.LINKED_CONTROLLER.asStack();
		stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, controllerData);
		return stack;
	}
}
