package com.simibubi.create.content.decoration.steamWhistle;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class WhistleRenderer extends SafeBlockEntityRenderer<WhistleBlockEntity> {

	public WhistleRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(WhistleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new WhistleRenderState();
	}

	@Override
	public void extractRenderState(WhistleBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof WhistleRenderState whistleState) {
			whistleState.blockEntity = be;
			whistleState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof WhistleRenderState whistleState))
			return;
		WhistleBlockEntity be = whistleState.blockEntity;
		if (be == null)
			return;

		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof WhistleBlock))
			return;

		Direction direction = blockState.getValue(WhistleBlock.FACING);
		WhistleSize size = blockState.getValue(WhistleBlock.SIZE);
		StandaloneModelKey<BlockStateModelPart> mouth = size == WhistleSize.LARGE
			? CreateStandaloneModels.WHISTLE_MOUTH_LARGE
			: size == WhistleSize.MEDIUM ? CreateStandaloneModels.WHISTLE_MOUTH_MEDIUM
				: CreateStandaloneModels.WHISTLE_MOUTH_SMALL;

		float offset = be.animation.getValue(whistleState.partialTicks);
		if (be.animation.getChaseTarget() > 0 && be.animation.getValue() > 0.5f) {
			float wiggleProgress = (AnimationTickHolder.getTicks() + whistleState.partialTicks) / 8f;
			offset -= Math.sin(wiggleProgress * (2 * Mth.PI) * (4 - size.ordinal())) / 16f;
		}

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
		ms.translate(-.5, -.5, -.5);
		ms.translate(0, offset * 4 / 16f, 0);
		submitPart(mouth, ms, collector, state.lightCoords);
		ms.popPose();
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static class WhistleRenderState extends BlockEntityRenderState {
		private WhistleBlockEntity blockEntity;
		private float partialTicks;
	}
}
