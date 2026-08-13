package com.simibubi.create.content.redstone.deskBell;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DeskBellRenderer extends SmartBlockEntityRenderer<DeskBellBlockEntity> {

	public DeskBellRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(DeskBellBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new DeskBellRenderState();
	}

	@Override
	public void extractRenderState(DeskBellBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof DeskBellRenderState bellState) {
			bellState.blockState = be.getBlockState();
			bellState.progress = be.animation.getValue(partialTicks);
			bellState.animationOffset = be.animationOffset;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof DeskBellRenderState bellState) || bellState.blockState == null)
			return;
		BlockState blockState = bellState.blockState;
		float p = bellState.progress;
		if (p < .004f && !blockState.getOptionalValue(DeskBellBlock.POWERED).orElse(false))
			return;

		BlockStateModelPart plunger = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.DESK_BELL_PLUNGER);
		BlockStateModelPart bell = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.DESK_BELL_BELL);
		if (plunger == null || bell == null)
			return;

		float press = (float) (1 - 4 * Math.pow(Math.max(p - .5f, 0) - .5f, 2));
		float swing = (float) Math.pow(p, 1.25f);
		Direction facing = blockState.getValue(DeskBellBlock.FACING);
		float yRot = AngleHelper.horizontalAngle(facing);
		float xRot = AngleHelper.verticalAngle(facing) + 90;

		ms.pushPose();
		orient(ms, yRot, xRot);
		ms.translate(0, press * -.75f / 16f, 0);
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(plunger),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
		ms.translate(0, -1 / 16f, 0);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
			swing * 8 * Mth.sin(p * Mth.PI * 4 + bellState.animationOffset)));
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(
			swing * 8 * Mth.cos(p * Mth.PI * 4 + bellState.animationOffset)));
		ms.translate(0, 1 / 16f, 0);
		ms.scale(.995f, .995f, .995f);
		ms.translate(-.5f, -.5f, -.5f);
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(bell),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void orient(PoseStack ms, float yRot, float xRot) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
		ms.translate(-.5f, -.5f, -.5f);
	}

	private static class DeskBellRenderState extends BlockEntityRenderState {
		private BlockState blockState;
		private float progress;
		private float animationOffset;
	}

}
