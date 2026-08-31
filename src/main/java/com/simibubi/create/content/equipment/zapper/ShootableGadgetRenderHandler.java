package com.simibubi.create.content.equipment.zapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public abstract class ShootableGadgetRenderHandler {

	public static void handlePacket(ShootGadgetPacket packet) {
		var camera = Minecraft.getInstance().getCameraEntity();
		if (camera == null || camera.position().distanceTo(packet.location) > 100)
			return;
		ShootableGadgetRenderHandler handler;
		if (packet instanceof ZapperBeamPacket beam) {
			handler = CreateClient.ZAPPER_RENDER_HANDLER;
			CreateClient.ZAPPER_RENDER_HANDLER.addBeam(new ZapperRenderHandler.LaserBeam(packet.location, beam.target()));
		} else if (packet instanceof PotatoCannonPacket cannon) {
			handler = CreateClient.POTATO_CANNON_RENDER_HANDLER;
			CreateClient.POTATO_CANNON_RENDER_HANDLER.beforeShoot(cannon.pitch(), packet.location, cannon.motion(),
				cannon.item());
		} else
			return;
		if (packet.self)
			handler.shoot(packet.hand, packet.location);
		else
			handler.playSound(packet.hand, packet.location);
	}

	protected float leftHandAnimation;
	protected float rightHandAnimation;
	protected float lastLeftHandAnimation;
	protected float lastRightHandAnimation;
	protected boolean dontReequipLeft;
	protected boolean dontReequipRight;
	private boolean renderingTool;

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

	public float getEquipProgress(boolean rightHand, float equipProgress) {
		boolean animating = rightHand ? rightHandAnimation > .01f : leftHandAnimation > .01f;
		boolean suppressReequip = rightHand ? dontReequipRight : dontReequipLeft;
		return animating || suppressReequip ? 0 : equipProgress;
	}

	public boolean isRenderingTool() {
		return renderingTool;
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
		float equipProgress = getEquipProgress(rightHand, event.getEquipProgress());

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		// Start with the native 26.2 first-person arm transform. The old renderer's
		// closer 1.21 coordinates make the submitted arm enormous in the new pipeline.
		poseStack.translate(flip * (xSwing + .64f), ySwing - .6f + equipProgress * -.6f,
			zSwing - .72f + recoil);
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * 45));
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * yRotation * 70));
		poseStack.mulPose(Axis.ZP.rotationDegrees(flip * zRotation * -20));
		poseStack.translate(flip * -1, 3.6, 3.5);
		poseStack.mulPose(Axis.ZP.rotationDegrees(flip * 120));
		poseStack.mulPose(Axis.XP.rotationDegrees(200));
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * -135));
		poseStack.translate(flip * 5.6, 0, 0);
		transformHand(poseStack, flip, equipProgress, recoil, event.getPartialTick());
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

		// Render the gadget in the same pass and cancel the native item pass. Letting
		// both paths run makes vanilla re-equip and custom recoil win on alternating frames.
		poseStack.pushPose();
		poseStack.translate(flip * (xSwing + .64f - .1f), ySwing - .4f + equipProgress * -.6f,
			zSwing - .72f - .1f + recoil);
		poseStack.mulPose(Axis.YP.rotationDegrees(flip * yRotation * 70));
		poseStack.mulPose(Axis.ZP.rotationDegrees(flip * zRotation * -20));
		transformTool(poseStack, flip, equipProgress, recoil, event.getPartialTick());
		ItemInHandRenderer itemRenderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
		ItemDisplayContext context = rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
			: ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		renderingTool = true;
		try {
			itemRenderer.renderItem(player, event.getItemStack(), context, poseStack, event.getSubmitNodeCollector(),
				event.getPackedLight());
		} finally {
			renderingTool = false;
		}
		poseStack.popPose();
		event.setCanceled(true);
	}

	public void dontAnimateItem(InteractionHand hand) {
		LocalPlayer player = Minecraft.getInstance().player;
		boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
		dontReequipRight |= rightHand;
		dontReequipLeft |= !rightHand;
	}

}
