package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

/**
 * Taken from EntityRendererManager
 */
public class ShadowRenderHelper {

	public static void renderShadow(PoseStack matrixStack, MultiBufferSource buffer, float opacity, float radius) {
	}

	public static void renderShadow(PoseStack matrixStack, MultiBufferSource buffer, LevelReader world,
		Vec3 pos, float opacity, float radius) {
	}

}
