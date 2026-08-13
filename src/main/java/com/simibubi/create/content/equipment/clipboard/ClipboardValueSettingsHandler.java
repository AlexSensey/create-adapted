package com.simibubi.create.content.equipment.clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber
public class ClipboardValueSettingsHandler {

	@SubscribeEvent
	public static void rightClickToCopy(RightClickBlock event) {
		if (event.getLevel()
			.isClientSide())
			return;
		if (copyToClipboard(event.getLevel(), event.getEntity(), event.getEntity()
			.getMainHandItem(), event.getHitVec())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}

	@SubscribeEvent
	public static void leftClickToPaste(LeftClickBlock event) {
		if (event.getAction() != LeftClickBlock.Action.START)
			return;
		if (applyClipboard(event.getLevel(), event.getEntity(), event.getEntity()
			.getMainHandItem(), new BlockHitResult(Vec3.atCenterOf(event.getPos()), event.getFace(), event.getPos(), false),
			false))
			event.setCanceled(true);
	}

	@EventBusSubscriber(value = Dist.CLIENT)
	public static class ClientEvents {
		public static void clientTick() {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.isSpectator())
				return;
			if (minecraft.level == null)
				return;
			if (!(minecraft.hitResult instanceof BlockHitResult target))
				return;

			ItemStack clipboard = minecraft.player.getMainHandItem();
			if (!AllBlocks.CLIPBOARD.isIn(clipboard))
				return;
			if (!(minecraft.level.getBlockEntity(target.getBlockPos()) instanceof SmartBlockEntity))
				return;

			boolean canCopy = hasCloneableValues(minecraft.level, target);
			boolean canPaste = applyClipboard(minecraft.level, minecraft.player, clipboard, target, true);
			if (!canCopy && !canPaste)
				return;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(CreateLang.translateDirect("clipboard.actions"));
			if (canCopy)
				tip.add(CreateLang.translateDirect("clipboard.to_copy", Component.keybind("key.use")));
			if (canPaste)
				tip.add(CreateLang.translateDirect("clipboard.to_paste", Component.keybind("key.attack")));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	private static boolean copyToClipboard(Level level, Player player, ItemStack clipboard, BlockHitResult hit) {
		if (!canUseClipboard(player, clipboard) || hit == null)
			return false;
		CompoundTag copiedValues = collectValues(level, player, hit);
		if (copiedValues.isEmpty())
			return false;
		if (!level.isClientSide()) {
			ClipboardContent content = clipboard.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
			clipboard.set(AllDataComponents.CLIPBOARD_CONTENT, content.setCopiedValues(copiedValues));
			AllSoundEvents.CLIPBOARD_CHECKMARK.playOnServer(level, player.blockPosition(), .5f, 1);
			showActionbar(player, CreateLang
				.translate("clipboard.copied_from", level.getBlockState(hit.getBlockPos())
					.getBlock()
					.getName()
					.withStyle(ChatFormatting.WHITE))
				.style(ChatFormatting.GREEN)
				.component());
		}
		return true;
	}

	private static boolean applyClipboard(Level level, Player player, ItemStack clipboard, BlockHitResult hit,
		boolean simulate) {
		if (!canUseClipboard(player, clipboard) || hit == null)
			return false;
		Optional<CompoundTag> copiedValues = clipboard.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT,
			ClipboardContent.EMPTY).copiedValues();
		if (copiedValues.isEmpty())
			return false;
		boolean applied = applyValues(level, player, hit, copiedValues.get(), simulate || level.isClientSide());
		if (!applied)
			return false;
		if (!level.isClientSide()) {
			AllSoundEvents.CLIPBOARD_CHECKMARK.playOnServer(level, player.blockPosition(), .5f, 1.25f);
			showActionbar(player, CreateLang
				.translate("clipboard.pasted_to", level.getBlockState(hit.getBlockPos())
					.getBlock()
					.getName()
					.withStyle(ChatFormatting.WHITE))
				.style(ChatFormatting.GREEN)
				.component());
		}
		return true;
	}

	private static boolean canUseClipboard(Player player, ItemStack clipboard) {
		return player != null && !player.isSpectator() && AllBlocks.CLIPBOARD.isIn(clipboard);
	}

	private static void showActionbar(Player player, Component component) {
		if (player instanceof ServerPlayer serverPlayer)
			serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(component));
	}

	private static boolean hasCloneableValues(Level level, BlockHitResult hit) {
		BlockEntity blockEntity = level.getBlockEntity(hit.getBlockPos());
		if (!(blockEntity instanceof SmartBlockEntity smartBlockEntity))
			return false;
		if (smartBlockEntity instanceof ClipboardCloneable)
			return true;
		for (BlockEntityBehaviour behaviour : smartBlockEntity.getAllBehaviours())
			if (getCloneable(behaviour, hit.getDirection()) != null)
				return true;
		return false;
	}

	private static CompoundTag collectValues(Level level, Player player, BlockHitResult hit) {
		CompoundTag copiedValues = new CompoundTag();
		BlockEntity blockEntity = level.getBlockEntity(hit.getBlockPos());
		if (!(blockEntity instanceof SmartBlockEntity smartBlockEntity))
			return copiedValues;
		for (BlockEntityBehaviour behaviour : smartBlockEntity.getAllBehaviours())
			writeCloneableToClipboard(level, hit, copiedValues, getCloneable(behaviour, hit.getDirection()));
		if (smartBlockEntity instanceof ClipboardCloneable cloneable)
			writeCloneableToClipboard(level, hit, copiedValues, cloneable);
		return copiedValues;
	}

	private static void writeCloneableToClipboard(Level level, BlockHitResult hit, CompoundTag copiedValues,
		ClipboardCloneable cloneable) {
		if (cloneable == null)
			return;
		CompoundTag valueTag = new CompoundTag();
		if (cloneable.writeToClipboard(level.registryAccess(), valueTag, hit.getDirection()))
			copiedValues.put(cloneable.getClipboardKey(), valueTag);
	}

	private static boolean applyValues(Level level, Player player, BlockHitResult hit, CompoundTag copiedValues,
		boolean simulate) {
		BlockEntity blockEntity = level.getBlockEntity(hit.getBlockPos());
		if (!(blockEntity instanceof SmartBlockEntity smartBlockEntity))
			return false;
		boolean applied = false;
		for (BlockEntityBehaviour behaviour : smartBlockEntity.getAllBehaviours())
			applied |= applyCloneableFromClipboard(level, player, hit, copiedValues, simulate,
				getCloneable(behaviour, hit.getDirection()));
		if (smartBlockEntity instanceof ClipboardCloneable cloneable)
			applied |= applyCloneableFromClipboard(level, player, hit, copiedValues, simulate, cloneable);
		return applied;
	}

	private static boolean applyCloneableFromClipboard(Level level, Player player, BlockHitResult hit,
		CompoundTag copiedValues, boolean simulate, ClipboardCloneable cloneable) {
		if (cloneable == null)
			return false;
		String key = cloneable.getClipboardKey();
		if (!copiedValues.contains(key))
			return false;
		return cloneable.readFromClipboard(level.registryAccess(), copiedValues.getCompoundOrEmpty(key), player,
			hit.getDirection(), simulate);
	}

	private static ClipboardCloneable getCloneable(BlockEntityBehaviour behaviour, Direction side) {
		if (behaviour instanceof SidedFilteringBehaviour sidedFilteringBehaviour)
			behaviour = sidedFilteringBehaviour.get(side);
		return behaviour instanceof ClipboardCloneable cloneable ? cloneable : null;
	}
}
