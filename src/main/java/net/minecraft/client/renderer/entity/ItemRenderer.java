package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemRenderer {
	public static VertexConsumer getFoilBuffer(MultiBufferSource buffer, RenderType renderType, boolean isItem,
		boolean glint) {
		return buffer.getBuffer(renderType);
	}

	public static VertexConsumer getFoilBufferDirect(MultiBufferSource buffer, RenderType renderType, boolean isItem,
		boolean glint) {
		return buffer.getBuffer(renderType);
	}

	public BakedModel getModel(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity, int seed) {
		return null;
	}

	public void render(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack,
		MultiBufferSource buffer, int light, int overlay, BakedModel model) {
	}
}
