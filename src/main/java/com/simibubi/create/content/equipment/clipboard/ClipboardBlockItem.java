package com.simibubi.create.content.equipment.clipboard;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe.SupportsItemCopying;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClipboardBlockItem extends BlockItem implements SupportsItemCopying {

	public ClipboardBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null)
			return InteractionResult.PASS;
		if (player.isShiftKeyDown())
			return super.useOn(context);
		if (context.getLevel()
			.getBlockEntity(context.getClickedPos()) instanceof SmartBlockEntity)
			return InteractionResult.PASS;
		return use(context.getLevel(), player, context.getHand());
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, Player pPlayer, ItemStack pStack,
		BlockState pState) {
		if (pLevel.isClientSide())
			return false;
		if (!(pLevel.getBlockEntity(pPos) instanceof ClipboardBlockEntity cbe))
			return false;
		cbe.notifyUpdate();
		return true;
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (hand == InteractionHand.OFF_HAND)
			return InteractionResult.PASS;

		player.getCooldowns()
			.addCooldown(heldItem, 10);
		if (world.isClientSide())
			CatnipServices.PLATFORM.executeOnClientOnly(() -> () ->
				ClipboardClient.openScreen(player, heldItem.getComponents(), null));
		ClipboardContent content = heldItem.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
		heldItem.set(AllDataComponents.CLIPBOARD_CONTENT, content.setType(ClipboardType.EDITING));

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, TooltipDisplay tooltipDisplay,
		Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, tooltipContext, tooltipDisplay, tooltipComponents, tooltipFlag);
		tooltipComponents.accept(Component.literal("Right click: copy block settings")
			.withStyle(ChatFormatting.GRAY));
		tooltipComponents.accept(Component.literal("Left click: paste block settings")
			.withStyle(ChatFormatting.GRAY));
	}

	public void registerModelOverrides() {
		CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ClipboardOverrides.registerModelOverridesClient(this));
	}

	@Override
	public DataComponentType<?> getComponentType() {
		return AllDataComponents.CLIPBOARD_CONTENT;
	}

}
