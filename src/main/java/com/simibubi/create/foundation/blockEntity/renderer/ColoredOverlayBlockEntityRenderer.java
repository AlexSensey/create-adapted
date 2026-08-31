package com.simibubi.create.foundation.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import com.simibubi.create.foundation.render.CreateVisualizationManager;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class ColoredOverlayBlockEntityRenderer<T extends BlockEntity> extends SafeBlockEntityRenderer<T> {

	public ColoredOverlayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
			int light, int overlay) {
	}

	protected abstract int getColor(T be, float partialTicks);

	protected abstract SuperByteBuffer getOverlayBuffer(T be);

	public static SuperByteBuffer render(SuperByteBuffer buffer, int color, int light) {
		return buffer.color(color).light(light);
	}

}
