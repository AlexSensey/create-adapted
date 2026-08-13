package com.simibubi.create.foundation.item;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public interface LayeredArmorItem extends CustomRenderedArmorItem {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	default void renderArmorPiece(HumanoidArmorLayer<?, ?, ?> layer, PoseStack poseStack,
								  MultiBufferSource bufferSource, LivingEntity entity, EquipmentSlot slot, int light,
								  HumanoidModel<?> originalModel, ItemStack stack) {
		// TODO 26.2: restore layered armor rendering.
	}

	String getArmorTextureLocation(LivingEntity entity, EquipmentSlot slot, ItemStack stack, int layer);
}
