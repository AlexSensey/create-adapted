package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class NetheriteBacktankFirstPersonRenderer {

	private static final Identifier BACKTANK_ARMOR_LOCATION =
		Create.asResource("textures/models/armor/netherite_diving_arm.png");
	private static boolean rendererActive;

	public static void clientTick() {
		Minecraft minecraft = Minecraft.getInstance();
		rendererActive = minecraft.player != null
			&& AllItems.NETHERITE_BACKTANK.isIn(minecraft.player.getItemBySlot(EquipmentSlot.CHEST));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderPlayerHand(RenderArmEvent event) {
		if (!rendererActive)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null)
			return;
		EntityRenderer<?, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(event.getAvatar());
		if (!(renderer instanceof AvatarRenderer<?> avatarRenderer))
			return;

		PlayerModel model = avatarRenderer.getModel();
		ModelPart sleeve = event.getArm() == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
		// First-person rendering prepares only the bare arm. Copy that exact pose to
		// the slightly larger sleeve geometry used by the diving-arm texture.
		sleeve.loadPose(event.getArmPart().storePose());
		event.getSubmitNodeCollector().submitModelPart(sleeve, event.getPoseStack(),
			RenderTypes.entitySolid(BACKTANK_ARMOR_LOCATION), LightCoordsUtil.FULL_BRIGHT,
			OverlayTexture.NO_OVERLAY, null);
		event.setCanceled(true);
	}

}
