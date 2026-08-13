package com.simibubi.create.content.equipment.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class CardboardArmorHandlerClient {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void playerRendersAsBoxWhenSneaking(RenderPlayerEvent.Pre event) {
		if (!(event.getRenderState() instanceof CardboardStealthRenderState cardboard)
			|| !cardboard.create$isCardboardStealth() || cardboard.create$getCardboardBox().isEmpty())
			return;

		event.setCanceled(true);
		AvatarRenderState state = (AvatarRenderState) event.getRenderState();
		PoseStack ms = event.getPoseStack();
		ms.pushPose();
		if (cardboard.create$isCardboardOnGround()) {
			float hop = Math.min(Math.abs(Mth.cos((state.ageInTicks % 256) / 2.0f)) * .125f,
				cardboard.create$getCardboardMovement() * 5);
			ms.translate(0, hop, 0);
		}
		ms.mulPose(Axis.YP.rotationDegrees(-state.bodyRot - 90));
		ms.translate(0, .45, 0);
		ms.scale(1.35f, 1.35f, 1.35f);
		cardboard.create$getCardboardBox().submit(ms, event.getSubmitNodeCollector(), state.lightCoords,
			OverlayTexture.NO_OVERLAY, state.outlineColor);
		ms.popPose();
	}
}
