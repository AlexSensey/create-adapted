package com.simibubi.create.content.trains.signal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.OverlayState;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CreateVisualizationManager;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import java.util.List;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class SignalRenderer extends SafeBlockEntityRenderer<SignalBlockEntity> {

	public SignalRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(SignalBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SignalRenderState();
	}

	@Override
	public void extractRenderState(SignalBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof SignalRenderState signalState)
			signalState.blockEntity = be;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SignalRenderState signalState))
			return;
		SignalBlockEntity be = signalState.blockEntity;
		if (be == null || be.getLevel() == null)
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		submitSignalLamp(be, ms, collector, state.lightCoords);

		OverlayState overlayState = be.getOverlay();
		if (overlayState == OverlayState.SKIP)
			return;

		RenderedTrackOverlayType type = overlayState == OverlayState.DUAL ? RenderedTrackOverlayType.DUAL_SIGNAL
			: RenderedTrackOverlayType.SIGNAL;

		renderTrackOverlay(ms, collector, be.getLevel(), be.getBlockPos(), be.edgePoint, type, state.lightCoords);
	}

	private static void submitSignalLamp(SignalBlockEntity be, PoseStack ms, SubmitNodeCollector collector, int light) {
		SignalState signalState = be.getState();
		boolean red = signalState.isRedLight(AnimationTickHolder.getRenderTime());
		StandaloneModelKey<BlockStateModelPart> key = red ? CreateStandaloneModels.SIGNAL_ON
			: CreateStandaloneModels.SIGNAL_OFF;
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, red ? LightCoordsUtil.pack(15, 15) : light, 0, 0);
	}

	private static void renderTrackOverlay(PoseStack ms, SubmitNodeCollector collector, Level level,
		BlockPos blockEntityPos, TrackTargetingBehaviour<?> target, RenderedTrackOverlayType type, int light) {
		BlockPos targetPosition = target.getGlobalPosition();
		ms.pushPose();
		BlockPos offset = targetPosition.subtract(blockEntityPos);
		ms.translate(offset.getX(), offset.getY(), offset.getZ());
		TrackTargetingClient.submitOverlay(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
			collector, light, type, 1);
		ms.popPose();
	}

	private static class SignalRenderState extends BlockEntityRenderState {
		private SignalBlockEntity blockEntity;
	}
}
