package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.Create;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CardboardArmorStealthOverlay implements IClientItemExtensions {
	private static final Identifier PACKAGE_BLUR = Create.asResource("textures/misc/package_blur.png");
	private static final LerpedFloat OPACITY = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, .25f, Chaser.EXP);

	public static void clientTick() {
		if (Minecraft.getInstance().player == null)
			return;
		OPACITY.tickChaser();
		OPACITY.updateChaseTarget(CardboardArmorHandler.testForStealth(Minecraft.getInstance().player) ? 1 : 0);
	}

	@Override
	public void renderFirstPersonOverlay(ItemStack stack, EquipmentSlot slot, Player player,
		GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (slot != EquipmentSlot.HEAD)
			return;
		float opacity = OPACITY.getValue(deltaTracker.getGameTimeDeltaPartialTick(false));
		if (opacity <= 1 / 255f)
			return;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		graphics.blit(RenderPipelines.GUI_TEXTURED, PACKAGE_BLUR, 0, 0, 0, 0, width, height, 256, 256,
			ARGB.white(opacity));
	}

}
