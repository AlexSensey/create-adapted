package com.simibubi.create.foundation.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface CustomArmPoseItem {
	boolean shouldUseCrossbowHold(ItemStack stack, LivingEntity player, InteractionHand hand);
}
