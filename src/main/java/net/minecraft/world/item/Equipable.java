package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface Equipable {
	EquipmentSlot getEquipmentSlot();

	default InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item item, Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		EquipmentSlot slot = getEquipmentSlot();
		ItemStack equipped = player.getItemBySlot(slot);
		player.setItemSlot(slot, stack.copyWithCount(1));
		stack.shrink(1);
		if (!equipped.isEmpty())
			player.getInventory().placeItemBackInInventory(equipped);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	static Equipable get(ItemStack stack) {
		return stack.getItem() instanceof Equipable equipable ? equipable : null;
	}
}
