package com.simibubi.create.content.processing.burner;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class BlazeBurnerClient {

	private BlazeBurnerClient() {}

	public static void tickAnimation(BlazeBurnerBlockEntity burner) {
		boolean active = burner.getHeatLevelFromBlock().isAtLeast(HeatLevel.FADING) && burner.isValidBlockAbove();
		float target = 0;
		if (!active) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && !player.isInvisible()) {
				double x = burner.isVirtual() ? -4 : player.getX();
				double z = burner.isVirtual() ? -10 : player.getZ();
				double dx = x - (burner.getBlockPos().getX() + 0.5);
				double dz = z - (burner.getBlockPos().getZ() + 0.5);
				target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
			}
		}
		burner.tickAnimation(target, active);
	}
}
