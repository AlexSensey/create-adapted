package com.simibubi.create.content.equipment.extendoGrip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllItems;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ExtendoGripRenderHandler {

	public static float mainHandAnimation;
	public static float lastMainHandAnimation;
	public static PartialModel pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;

	public static void tick() {
		lastMainHandAnimation = mainHandAnimation;
		mainHandAnimation *= Mth.clamp(mainHandAnimation, .8f, .99f);
		pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;
		var player = Minecraft.getInstance().player;
		if (player != null && AllItems.EXTENDO_GRIP.isIn(player.getOffhandItem())
			&& player.getMainHandItem().getItem() instanceof BlockItem)
			pose = AllPartialModels.DEPLOYER_HAND_HOLDING;
	}

	@SubscribeEvent
	public static void onRenderPlayerHand(RenderHandEvent event) {
		var player = Minecraft.getInstance().player;
		if (player == null)
			return;

		ItemStack renderedStack = event.getItemStack();
		ItemStack offhandStack = player.getOffhandItem();
		boolean extendoInOffhand = AllItems.EXTENDO_GRIP.isIn(offhandStack);
		boolean renderingExtendo = AllItems.EXTENDO_GRIP.isIn(renderedStack);
		if (!extendoInOffhand && !renderingExtendo)
			return;

		// When the grip is in the offhand, the complete assembly is rendered during
		// the main-hand pass. Suppress the ordinary offhand pass to avoid a duplicate.
		if (event.getHand() != InteractionHand.MAIN_HAND) {
			if (extendoInOffhand)
				event.setCanceled(true);
			return;
		}

		if (event.getSwingProgress() > 0 && 1 - event.getSwingProgress() > mainHandAnimation)
			mainHandAnimation = .95f;
		boolean rightHand = player.getMainArm() == HumanoidArm.RIGHT;
		float flip = rightHand ? 1 : -1;
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		// Keep the grip anchored while its own mechanism extends. In 26.2 the
		// attack-miss path also changes equipProgress and otherwise pushes the
		// complete custom-rendered assembly down a second time.
		poseStack.translate(flip * .54000005f, -.4f, -.41999996f);

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * 75));
		poseStack.translate(flip * -1, 3.6f, 3.5f);
		poseStack.mulPose(Axis.ZP.rotationDegrees(flip * 120));
		poseStack.mulPose(Axis.XP.rotationDegrees(200));
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * -135));
		poseStack.translate(flip * 5.6f, 0, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * 40));
		poseStack.translate(flip * .05f, -.3f, -.3f);
		if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof AvatarRenderer avatarRenderer) {
			if (rightHand)
				avatarRenderer.renderRightHand(poseStack, event.getSubmitNodeCollector(), event.getPackedLight(),
					player.getSkin().body().texturePath(), false);
			else
				avatarRenderer.renderLeftHand(poseStack, event.getSubmitNodeCollector(), event.getPackedLight(),
					player.getSkin().body().texturePath(), false);
		}
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(flip * -.1f, 0, -.3f);
		ItemInHandRenderer itemRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
		ItemDisplayContext context = rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
			: ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		ItemStack gripStack = extendoInOffhand ? offhandStack : renderedStack;
		itemRenderer.renderItem(player, gripStack, context, poseStack, event.getSubmitNodeCollector(),
			event.getPackedLight());

		poseStack.popPose();
		poseStack.popPose();
		event.setCanceled(true);
	}
}
