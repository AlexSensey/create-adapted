package com.simibubi.create.content.trains.observer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CreateVisualizationManager;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrackObserverRenderer extends SmartBlockEntityRenderer<TrackObserverBlockEntity> {

	public TrackObserverRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TrackObserverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new TrackObserverRenderState();
	}

	@Override
	public void extractRenderState(TrackObserverBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
		if (state instanceof TrackObserverRenderState observerState)
			observerState.blockEntity = be;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof TrackObserverRenderState observerState))
			return;
		TrackObserverBlockEntity be = observerState.blockEntity;
		if (be == null || be.getLevel() == null)
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		renderTrackOverlay(ms, collector, be.getLevel(), be.getBlockPos(), be.edgePoint.getGlobalPosition(),
			be.edgePoint.getTargetBezier(), be.edgePoint.getTargetDirection(), state.lightCoords);
		renderFilterItem(be, ms, collector, state.lightCoords);
	}

	private static void renderFilterItem(TrackObserverBlockEntity be, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		FilteringBehaviour filtering = be.getBehaviour(FilteringBehaviour.TYPE);
		if (filtering == null || !filtering.isActive())
			return;
		ItemStack filter = filtering.getFilter();
		if (filter.isEmpty())
			return;
		Vec3 offset = filtering.getSlotPositioning()
			.getLocalOffset(be.getLevel(), be.getBlockPos(), be.getBlockState());
		if (offset == null)
			return;

		ms.pushPose();
		ms.translate(offset.x, offset.y + 1 / 32d, offset.z);
		ms.mulPose(Axis.XP.rotationDegrees(270));
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .25f);
		ms.popPose();
	}

	private static void renderTrackOverlay(PoseStack ms, SubmitNodeCollector collector, Level level,
		BlockPos blockEntityPos, BlockPos targetPosition,
		com.simibubi.create.content.trains.track.BezierTrackPointLocation bezier, AxisDirection direction, int light) {
		ms.pushPose();
		BlockPos offset = targetPosition.subtract(blockEntityPos);
		ms.translate(offset.getX(), offset.getY(), offset.getZ());
		TrackTargetingClient.submitOverlay(level, targetPosition, direction, bezier, ms, collector, light,
			RenderedTrackOverlayType.OBSERVER, 1);
		ms.popPose();
	}

	private static class TrackObserverRenderState extends SmartRenderState {
		private TrackObserverBlockEntity blockEntity;
	}
}
