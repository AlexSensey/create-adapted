package com.simibubi.create.foundation.render;

import java.util.BitSet;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import com.simibubi.create.foundation.render.CreateVisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlockEntityRenderHelper {
	public static void renderBlockEntities(List<BlockEntity> blockEntities, BitSet shouldRenderBEs, BitSet erroredBEsOut,
		@javax.annotation.Nullable VirtualRenderWorld renderLevel, Level realLevel, PoseStack ms,
		@javax.annotation.Nullable Matrix4f lightTransform, MultiBufferSource buffer, float pt) {
		for (int i = shouldRenderBEs.nextSetBit(0); i >= 0 && i < blockEntities.size(); i = shouldRenderBEs.nextSetBit(i + 1)) {
			BlockEntity blockEntity = blockEntities.get(i);
			if (CreateVisualizationManager.supportsVisualization(realLevel)
				&& VisualizationHelper.skipVanillaRender(blockEntity))
				continue;
			BlockEntityRenderer<BlockEntity, ?> renderer = Minecraft.getInstance()
				.getBlockEntityRenderDispatcher()
				.getRenderer(blockEntity);
			if (!(renderer instanceof SafeBlockEntityRenderer<?> safeRenderer)) {
				erroredBEsOut.set(i);
				continue;
			}

			BlockPos pos = blockEntity.getBlockPos();
			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			try {
				int realLevelLight = LightCoordsUtil.getLightCoords(realLevel, getLightPos(lightTransform, pos));
				int light = realLevelLight;
				if (renderLevel != null) {
					renderLevel.setExternalLight(realLevelLight);
					light = LightCoordsUtil.getLightCoords(renderLevel, pos);
				}

				@SuppressWarnings({ "rawtypes", "unchecked" })
				SafeBlockEntityRenderer rawRenderer = safeRenderer;
				rawRenderer.render(blockEntity, pt, ms, buffer, light, OverlayTexture.NO_OVERLAY);
			} catch (Exception e) {
				erroredBEsOut.set(i);
			}

			ms.popPose();
		}

		if (renderLevel != null)
			renderLevel.resetExternalLight();
	}

	private static BlockPos getLightPos(@javax.annotation.Nullable Matrix4f lightTransform, BlockPos contraptionPos) {
		if (lightTransform == null)
			return contraptionPos;

		Vector4f lightVec =
			new Vector4f(contraptionPos.getX() + .5f, contraptionPos.getY() + .5f, contraptionPos.getZ() + .5f, 1);
		lightVec.mul(lightTransform);
		return BlockPos.containing(lightVec.x(), lightVec.y(), lightVec.z());
	}
}
