package com.simibubi.create.content.equipment.zapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public abstract class ShootableGadgetRenderHandler {

	protected float leftHandAnimation;
	protected float rightHandAnimation;
	protected float lastLeftHandAnimation;
	protected float lastRightHandAnimation;
	protected boolean dontReequipLeft;
	protected boolean dontReequipRight;

	public void tick() {
		lastLeftHandAnimation = leftHandAnimation;
		lastRightHandAnimation = rightHandAnimation;
		leftHandAnimation *= animationDecay();
		rightHandAnimation *= animationDecay();
	}

	public float getAnimation(boolean rightHand, float partialTicks) {
		return Mth.lerp(partialTicks, rightHand ? lastRightHandAnimation : lastLeftHandAnimation,
			rightHand ? rightHandAnimation : leftHandAnimation);
	}

	protected float animationDecay() {
		return 0.8f;
	}

	public void shoot(InteractionHand hand, Vec3 location) {
		LocalPlayer player = Minecraft.getInstance().player;
		boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
		if (rightHand) {
			rightHandAnimation = .2f;
			dontReequipRight = false;
		} else {
			leftHandAnimation = .2f;
			dontReequipLeft = false;
		}
		playSound(hand, location);
	}

	protected abstract void playSound(InteractionHand hand, Vec3 position);

	protected abstract boolean appliesTo(ItemStack stack);

	protected abstract void transformTool(PoseStack ms, float flip, float equipProgress, float recoil, float pt);

	protected abstract void transformHand(PoseStack ms, float flip, float equipProgress, float recoil, float pt);

	public void registerListeners(IEventBus bus) {
		bus.addListener(this::onRenderPlayerHand);
	}

	protected void onRenderPlayerHand(RenderHandEvent event) {
		if (!appliesTo(event.getItemStack()))
			return;
		Minecraft mc = Minecraft.getInstance();
		AbstractClientPlayer player = mc.player;
		if (player == null || player.isInvisible())
			return;

		boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND
			^ player.getMainArm() == HumanoidArm.LEFT;
		float flip = rightHand ? 1 : -1;
		float swing = event.getSwingProgress();
		float swingRoot = Mth.sqrt(swing);
		float xSwing = -.3f * Mth.sin(swingRoot * Mth.PI);
		float ySwing = .4f * Mth.sin(swingRoot * Mth.TWO_PI);
		float zSwing = -.4f * Mth.sin(swing * Mth.PI);
		float zRotation = Mth.sin(swing * swing * Mth.PI);
		float yRotation = Mth.sin(swingRoot * Mth.PI);
		float recoil = rightHand
			? Mth.lerp(event.getPartialTick(), lastRightHandAnimation, rightHandAnimation)
			: Mth.lerp(event.getPartialTick(), lastLeftHandAnimation, leftHandAnimation);

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		// Start with the native 26.2 first-person arm transform. The old renderer's
		// closer 1.21 coordinates make the submitted arm enormous in the new pipeline.
		poseStack.translate(flip * (xSwing + .64f), ySwing - .6f + event.getEquipProgress() * -.6f,
			zSwing - .72f + recoil);
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * 45));
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * yRotation * 70));
		poseStack.mulPose(Axis.ZP.rotationDegrees(flip * zRotation * -20));
		poseStack.translate(flip * -1, 3.6, 3.5);
		poseStack.mulPose(Axis.ZP.rotationDegrees(flip * 120));
		poseStack.mulPose(Axis.XP.rotationDegrees(200));
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * -135));
		poseStack.translate(flip * 5.6, 0, 0);
		transformHand(poseStack, flip, event.getEquipProgress(), recoil, event.getPartialTick());
		// Submitted avatar parts are larger in the 26.2 pipeline than the former
		// immediate-mode hand mesh. Scale only the mesh, preserving the grip position.
		poseStack.scale(.625f, .625f, .625f);

		AvatarRenderer<AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
		var skin = player.getSkin().body().texturePath();
		if (rightHand)
			renderer.renderRightHand(poseStack, event.getSubmitNodeCollector(), event.getPackedLight(), skin,
				player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
		else
			renderer.renderLeftHand(poseStack, event.getSubmitNodeCollector(), event.getPackedLight(), skin,
				player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
		poseStack.popPose();
	}

	public void dontAnimateItem(InteractionHand hand) {
		LocalPlayer player = Minecraft.getInstance().player;
		boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
		dontReequipRight |= rightHand;
		dontReequipLeft |= !rightHand;
	}

}
