package com.simibubi.create.content.equipment.zapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public abstract class ZapperItemRenderer extends CustomRenderedItemModelRenderer {

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
		ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		// TODO 26.2: restore zapper item renderer.
	}

	protected float getAnimationProgress(float pt, boolean leftHanded, boolean mainHand) {
		float animation = CreateClient.ZAPPER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded, pt);
		return Mth.clamp(animation * 5, 0, 1);
	}

	protected float getAnimationProgress(boolean leftHanded, boolean mainHand) {
		return getAnimationProgress(AnimationTickHolder.getPartialTicks(), leftHanded, mainHand);
	}

}
