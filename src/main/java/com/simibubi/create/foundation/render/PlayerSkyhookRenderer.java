package com.simibubi.create.foundation.render;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.AllTags.AllItemTags;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public class PlayerSkyhookRenderer {

	private static final Set<UUID> hangingPlayers = new HashSet<>();
	private static final Set<Integer> hangingPlayerIds = new HashSet<>();

	public static void updatePlayerList(Collection<UUID> uuids) {
		hangingPlayers.clear();
		hangingPlayers.addAll(uuids);
		hangingPlayerIds.clear();

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;

		for (UUID uuid : uuids) {
			Player player = level.getPlayerByUUID(uuid);
			if (player != null)
				hangingPlayerIds.add(player.getId());
		}
	}

	public static void setLocalPlayerHanging(boolean hanging) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null)
			return;

		if (hanging) {
			hangingPlayers.add(player.getUUID());
			hangingPlayerIds.add(player.getId());
			return;
		}

		hangingPlayers.remove(player.getUUID());
		hangingPlayerIds.remove(player.getId());
	}

	public static void beforeSetupAnim(Player player, HumanoidModel<?> model) {
		if (hangingPlayers.contains(player.getUUID()))
			return;

		model.head.resetPose();
		model.hat.resetPose();
		model.body.resetPose();
		model.leftArm.resetPose();
		model.rightArm.resetPose();
		model.leftLeg.resetPose();
		model.rightLeg.resetPose();
	}

	public static void afterSetupAnim(Player player, HumanoidModel<?> model) {
		if (hangingPlayers.contains(player.getUUID()))
			setHangingPose(player.getMainArm() == HumanoidArm.LEFT ^
				!AllItemTags.CHAIN_RIDEABLE.matches(player.getMainHandItem()), model);
	}

	public static void beforeSetupAnim(HumanoidRenderState renderState, HumanoidModel<?> model) {
		if (!(renderState instanceof AvatarRenderState avatarRenderState))
			return;
		if (hangingPlayerIds.contains(avatarRenderState.id))
			return;

		model.head.resetPose();
		model.hat.resetPose();
		model.body.resetPose();
		model.leftArm.resetPose();
		model.rightArm.resetPose();
		model.leftLeg.resetPose();
		model.rightLeg.resetPose();
	}

	public static void afterSetupAnim(HumanoidRenderState renderState, HumanoidModel<?> model) {
		if (!(renderState instanceof AvatarRenderState avatarRenderState))
			return;
		if (hangingPlayerIds.contains(avatarRenderState.id))
			setHangingPose(renderState.mainArm == HumanoidArm.LEFT ^
				!AllItemTags.CHAIN_RIDEABLE.matches(renderState.getMainHandItemStack()), model);
	}

	private static void setHangingPose(boolean isLeftArmMain, HumanoidModel<?> model) {
		if (Minecraft.getInstance().isPaused())
			return;

		model.head.x = 0;
		model.hat.x = 0;
		model.body.resetPose();
		model.leftArm.resetPose();
		model.rightArm.resetPose();
		model.leftLeg.resetPose();
		model.rightLeg.resetPose();

		float time = AnimationTickHolder.getTicks(true) + AnimationTickHolder.getPartialTicks();
		float mainCycle = Mth.sin(((float) ((time + 10) * 0.3f / Math.PI)));
		float limbCycle = Mth.sin(((float) (time * 0.3f / Math.PI)));
		float bodySwing = AngleHelper.rad(15 + (mainCycle * 10));
		float limbSwing = AngleHelper.rad(limbCycle * 15);
		if (isLeftArmMain) bodySwing = -bodySwing;
		model.body.zRot = bodySwing;
		model.head.zRot = bodySwing;
		model.hat.zRot = bodySwing;

		ModelPart hangingArm = isLeftArmMain ? model.leftArm : model.rightArm;
		ModelPart otherArm = isLeftArmMain ? model.rightArm : model.leftArm;
		hangingArm.y -= 3;

		float offsetX = hangingArm.x;
		float offsetY = hangingArm.y;
//		model.rightArm.x = offsetX * Mth.cos(bodySwing) - offsetY * Mth.sin(bodySwing);
//		model.rightArm.y = offsetX * Mth.sin(bodySwing) + offsetY * Mth.cos(bodySwing);
		float armPivotX = offsetX * Mth.cos(bodySwing) - offsetY * Mth.sin(bodySwing) + (isLeftArmMain ? -1 : 1) * 4.5f;
		float armPivotY = offsetX * Mth.sin(bodySwing) + offsetY * Mth.cos(bodySwing) + 2;
		hangingArm.xRot = -AngleHelper.rad(150);
		hangingArm.zRot = (isLeftArmMain ? -1 : 1) * AngleHelper.rad(15);

		offsetX = otherArm.x;
		offsetY = otherArm.y;
		otherArm.x = offsetX * Mth.cos(bodySwing) - offsetY * Mth.sin(bodySwing);
		otherArm.y = offsetX * Mth.sin(bodySwing) + offsetY * Mth.cos(bodySwing);
		otherArm.zRot = (isLeftArmMain ? -1 : 1) * (-AngleHelper.rad(20)) + 0.5f * bodySwing + limbSwing;

		ModelPart leadingLeg = isLeftArmMain ? model.leftLeg : model.rightLeg;
		ModelPart trailingLeg = isLeftArmMain ? model.rightLeg : model.leftLeg;

		leadingLeg.y -= 0.2f;
		offsetX = leadingLeg.x;
		offsetY = leadingLeg.y;
		leadingLeg.x = offsetX * Mth.cos(bodySwing) - offsetY * Mth.sin(bodySwing);
		leadingLeg.y = offsetX * Mth.sin(bodySwing) + offsetY * Mth.cos(bodySwing);
		leadingLeg.xRot = -AngleHelper.rad(25);
		leadingLeg.zRot = (isLeftArmMain ? -1 : 1) * (AngleHelper.rad(10)) + 0.5f * bodySwing + limbSwing;
		trailingLeg.y -= 0.8f;
		offsetX = trailingLeg.x;
		offsetY = trailingLeg.y;
		trailingLeg.x = offsetX * Mth.cos(bodySwing) - offsetY * Mth.sin(bodySwing);
		trailingLeg.y = offsetX * Mth.sin(bodySwing) + offsetY * Mth.cos(bodySwing);
		trailingLeg.xRot = AngleHelper.rad(10);
		trailingLeg.zRot = (isLeftArmMain ? -1 : 1) * (-AngleHelper.rad(10)) + 0.5f * bodySwing + limbSwing;
		model.hat.x -= armPivotX;
		model.head.x -= armPivotX;
		model.body.x -= armPivotX;
		otherArm.x -= armPivotX;
		trailingLeg.x -= armPivotX;
		leadingLeg.x -= armPivotX;

		model.hat.y -= armPivotY;
		model.head.y -= armPivotY;
		model.body.y -= armPivotY;
		otherArm.y -= armPivotY;
		trailingLeg.y -= armPivotY;
		leadingLeg.y -= armPivotY;
	}

}
