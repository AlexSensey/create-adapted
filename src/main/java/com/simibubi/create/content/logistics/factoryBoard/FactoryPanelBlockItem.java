package com.simibubi.create.content.logistics.factoryBoard;

import java.util.UUID;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FactoryPanelBlockItem extends LogisticallyLinkedBlockItem {

	public FactoryPanelBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext pContext) {
		ItemStack stack = pContext.getItemInHand();
		if (!isTuned(stack)) {
			AllSoundEvents.DENY.playOnServer(pContext.getLevel(), pContext.getClickedPos());
			Player player = pContext.getPlayer();
			if (player != null)
				player.sendOverlayMessage(CreateLang.translate("factory_panel.tune_before_placing")
					.component());
			return InteractionResult.FAIL;
		}
		return super.place(pContext);
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack,
		BlockState state) {
		return super.updateCustomBlockEntityTag(pos, level, player, fixCtrlCopiedStack(stack), state);
	}

	public static ItemStack fixCtrlCopiedStack(ItemStack stack) {
		// A Ctrl-picked panel stores its frequencies inside the individual panel
		// tags. Convert one of them back into the top-level linked-item format
		// before placement, matching the old Factory Gauge behaviour.
		if (isTuned(stack) && networkFromStack(stack) == null) {
			TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
			CompoundTag blockEntityTag = data == null ? new CompoundTag() : data.copyTagWithoutId();
			UUID frequency = null;

			for (PanelSlot slot : PanelSlot.values()) {
				String value = blockEntityTag.getCompoundOrEmpty(CreateLang.asId(slot.name()))
					.getStringOr("Freq", "");
				if (value.isEmpty())
					continue;
				try {
					frequency = UUID.fromString(value);
					break;
				} catch (IllegalArgumentException ignored) {
				}
			}

			if (frequency == null)
				frequency = UUID.randomUUID();
			CompoundTag linkedTag = new CompoundTag();
			linkedTag.putString("Freq", frequency.toString());
			BlockEntityType<?> type = ((IBE<?>) ((BlockItem) stack.getItem()).getBlock()).getBlockEntityType();
			stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(type, linkedTag));
		}
		return stack;
	}

}
