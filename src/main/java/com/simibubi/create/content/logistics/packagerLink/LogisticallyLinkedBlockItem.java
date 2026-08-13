package com.simibubi.create.content.logistics.packagerLink;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LogisticallyLinkedBlockItem extends BlockItem {

	public LogisticallyLinkedBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	@Override
	public boolean isFoil(@NotNull ItemStack pStack) {
		return isTuned(pStack);
	}

	public static boolean isTuned(ItemStack pStack) {
		return pStack.has(DataComponents.BLOCK_ENTITY_DATA);
	}

	@Nullable
	public static UUID networkFromStack(ItemStack pStack) {
		TypedEntityData<BlockEntityType<?>> data = pStack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data == null)
			return null;
		String frequency = data.copyTagWithoutId().getStringOr("Freq", "");
		if (frequency.isEmpty())
			return null;
		try {
			return UUID.fromString(frequency);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext tooltipContext,
		TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, tooltipContext, tooltipDisplay, tooltipComponents, tooltipFlag);
		if (networkFromStack(stack) == null)
			return;
		tooltipComponents.accept(CreateLang.translate("logistically_linked.tooltip")
			.style(ChatFormatting.GOLD)
			.component());
		tooltipComponents.accept(CreateLang.translate("logistically_linked.tooltip_clear")
			.style(ChatFormatting.GRAY)
			.component());
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
		ItemStack stack = player.getItemInHand(usedHand);
		if (isTuned(stack)) {
			if (level.isClientSide()) {
				level.playSound(player, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.75f, 1.0f);
			} else {
				player.sendOverlayMessage(CreateLang.translateDirect("logistically_linked.cleared"));
				stack.remove(DataComponents.BLOCK_ENTITY_DATA);
			}
			return InteractionResult.SUCCESS;
		}
		return super.use(level, player, usedHand);
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext pContext) {
		ItemStack stack = pContext.getItemInHand();
		BlockPos pos = pContext.getClickedPos();
		Level level = pContext.getLevel();
		Player player = pContext.getPlayer();
		InteractionHand hand = pContext.getHand();

		if (player == null)
			return InteractionResult.FAIL;
		if (player.isShiftKeyDown())
			return super.useOn(pContext);

		LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(level, pos, LogisticallyLinkedBehaviour.TYPE);
		boolean tuned = isTuned(stack);

		if (link != null) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			if (!link.mayInteractMessage(player))
				return InteractionResult.SUCCESS;

			assignFrequency(stack, player, link.freqId);
			return InteractionResult.SUCCESS;
		}

		InteractionResult useOn = super.useOn(pContext);
		if (level.isClientSide() || useOn == InteractionResult.FAIL)
			return useOn;

		player.sendOverlayMessage(CreateLang.translateDirect(tuned ? "logistically_linked.connected"
			: "logistically_linked.new_network_started"));
		return useOn;
	}

	public static void assignFrequency(ItemStack stack, Player player, UUID frequency) {
		CompoundTag tag = new CompoundTag();
		TypedEntityData<BlockEntityType<?>> current = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (current != null)
			tag = current.copyTagWithoutId();
		tag.putString("Freq", frequency.toString());
		BlockEntityType<?> type = ((IBE<?>) ((BlockItem) stack.getItem()).getBlock()).getBlockEntityType();
		stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(type, tag));
		player.sendOverlayMessage(CreateLang.translateDirect("logistically_linked.tuned"));
	}

}
