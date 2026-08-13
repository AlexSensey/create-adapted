package com.simibubi.create.content.trains.track;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.track.BezierConnection.GirderAngles;
import com.simibubi.create.content.trains.track.BezierConnection.SegmentAngles;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
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
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrackRenderer extends SafeBlockEntityRenderer<TrackBlockEntity> {

	private BlockStateModelPart tieModel;
	private BlockStateModelPart leftSegmentModel;
	private BlockStateModelPart rightSegmentModel;
	private BlockStateModelPart girderTopModel;
	private BlockStateModelPart girderMiddleModel;
	private BlockStateModelPart girderBottomModel;

	public TrackRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new TrackRenderState();
	}

	@Override
	public void extractRenderState(TrackBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof TrackRenderState trackState)
			trackState.blockEntity = be;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof TrackRenderState trackState))
			return;
		TrackBlockEntity be = trackState.blockEntity;
		if (be == null || be.isRemoved() || be.getLevel() == null)
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		Level level = level(be);
		be.connections.values()
			.forEach(bc -> submitBezierTurn(level, bc, ms, collector, this, cameraRenderState.pos));
	}

	@Override
	protected void renderSafe(TrackBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		VertexConsumer vb = buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock());
		be.connections.values()
			.forEach(bc -> renderBezierTurn(level(be), bc, ms, vb, this));
	}

	public static void renderBezierTurn(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb) {
		renderBezierTurn(level, bc, ms, vb, null);
	}

	public static void submitCurves(PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		TrackRenderer renderer = new TrackRenderer(null);
		Vec3 camera = cameraRenderState.pos;
		Map<BlockPos, TrackBlockEntity> tracksWithTurns = TrackBlockOutline.TRACKS_WITH_TURNS.get(mc.level);
		tracksWithTurns
			.values()
			.forEach(be -> {
				if (be == null || be.isRemoved() || be.getLevel() == null)
					return;
				ms.pushPose();
				BlockPos pos = be.getBlockPos();
				ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
				be.connections.values()
					.forEach(bc -> submitBezierTurn(mc.level, bc, ms, collector, renderer, camera));
				ms.popPose();
			});
	}

	public static void submitBezierTurn(Level level, BezierConnection bc, PoseStack ms, SubmitNodeCollector collector,
		TrackRenderer renderer, Vec3 camera) {
		if (!bc.isPrimary())
			return;

		SegmentAngles segment = bc.getBakedSegments();

		ms.pushPose();
		BlockPos bePosition = bc.bePositions.getFirst();

		submitGirder(level, bc, ms, collector, bePosition, renderer);

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			submitPart(ms, segment.tieTransform[i], collector, renderer.getTieModel(), light);

			for (boolean first : Iterate.trueAndFalse) {
				Pose transform = segment.railTransforms[i].get(first);
				submitPart(ms, transform, collector,
					first ? renderer.getLeftSegmentModel() : renderer.getRightSegmentModel(), light);
			}
		}

		ms.popPose();
	}

	private static void submitGirder(Level level, BezierConnection bc, PoseStack ms, SubmitNodeCollector collector,
		BlockPos bePosition, TrackRenderer renderer) {
		if (!bc.hasGirder)
			return;

		GirderAngles segment = bc.getBakedGirders();

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			for (boolean first : Iterate.trueAndFalse) {
				Pose beamTransform = segment.beams[i].get(first);
				submitPart(ms, beamTransform, collector, renderer.getGirderMiddleModel(), light);

				for (boolean top : Iterate.trueAndFalse) {
					Pose beamCapTransform = segment.beamCaps[i].get(top)
						.get(first);
					submitPart(ms, beamCapTransform, collector,
						top ? renderer.getGirderTopModel() : renderer.getGirderBottomModel(), light);
				}
			}
		}
	}

	private static void renderBezierTurn(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb,
		TrackRenderer renderer) {
		if (!bc.isPrimary())
			return;

		ms.pushPose();
		BlockPos bePosition = bc.bePositions.getFirst();
		SegmentAngles segment = bc.getBakedSegments();

		renderGirder(level, bc, ms, vb, bePosition, renderer);

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			renderPart(ms, segment.tieTransform[i], vb, renderer == null ? loadTieModel() : renderer.getTieModel(), light);

			for (boolean first : Iterate.trueAndFalse) {
				Pose transform = segment.railTransforms[i].get(first);
				renderPart(ms, transform, vb,
					renderer == null ? (first ? loadLeftSegmentModel() : loadRightSegmentModel())
						: (first ? renderer.getLeftSegmentModel() : renderer.getRightSegmentModel()),
					light);
			}
		}

		ms.popPose();
	}

	private static void renderBezierTurn(Level level, BezierConnection bc, Pose root, VertexConsumer vb,
		TrackRenderer renderer) {
		if (!bc.isPrimary())
			return;

		BlockPos bePosition = bc.bePositions.getFirst();
		SegmentAngles segment = bc.getBakedSegments();

		renderGirder(level, bc, root, vb, bePosition, renderer);

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			renderPart(root, segment.tieTransform[i], vb, renderer.getTieModel(), light);

			for (boolean first : Iterate.trueAndFalse) {
				Pose transform = segment.railTransforms[i].get(first);
				renderPart(root, transform, vb,
					first ? renderer.getLeftSegmentModel() : renderer.getRightSegmentModel(), light);
			}
		}
	}

	private static void renderGirder(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb,
		BlockPos bePosition, TrackRenderer renderer) {
		if (!bc.hasGirder)
			return;

		GirderAngles segment = bc.getBakedGirders();

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			for (boolean first : Iterate.trueAndFalse) {
				Pose beamTransform = segment.beams[i].get(first);
				renderPart(ms, beamTransform, vb,
					renderer == null ? loadGirderMiddleModel() : renderer.getGirderMiddleModel(), light);

				for (boolean top : Iterate.trueAndFalse) {
					Pose beamCapTransform = segment.beamCaps[i].get(top)
						.get(first);
					renderPart(ms, beamCapTransform, vb,
						renderer == null ? (top ? loadGirderTopModel() : loadGirderBottomModel())
							: (top ? renderer.getGirderTopModel() : renderer.getGirderBottomModel()),
						light);
				}
			}
		}
	}

	private static void renderGirder(Level level, BezierConnection bc, Pose root, VertexConsumer vb,
		BlockPos bePosition, TrackRenderer renderer) {
		if (!bc.hasGirder)
			return;

		GirderAngles segment = bc.getBakedGirders();

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			for (boolean first : Iterate.trueAndFalse) {
				Pose beamTransform = segment.beams[i].get(first);
				renderPart(root, beamTransform, vb, renderer.getGirderMiddleModel(), light);

				for (boolean top : Iterate.trueAndFalse) {
					Pose beamCapTransform = segment.beamCaps[i].get(top)
						.get(first);
					renderPart(root, beamCapTransform, vb,
						top ? renderer.getGirderTopModel() : renderer.getGirderBottomModel(), light);
				}
			}
		}
	}

	private BlockStateModelPart getTieModel() {
		return tieModel = tieModel == null ? loadTieModel() : tieModel;
	}

	private BlockStateModelPart getLeftSegmentModel() {
		return leftSegmentModel = leftSegmentModel == null ? loadLeftSegmentModel() : leftSegmentModel;
	}

	private BlockStateModelPart getRightSegmentModel() {
		return rightSegmentModel = rightSegmentModel == null ? loadRightSegmentModel() : rightSegmentModel;
	}

	private BlockStateModelPart getGirderTopModel() {
		return girderTopModel = girderTopModel == null ? loadGirderTopModel() : girderTopModel;
	}

	private BlockStateModelPart getGirderMiddleModel() {
		return girderMiddleModel = girderMiddleModel == null ? loadGirderMiddleModel() : girderMiddleModel;
	}

	private BlockStateModelPart getGirderBottomModel() {
		return girderBottomModel = girderBottomModel == null ? loadGirderBottomModel() : girderBottomModel;
	}

	private static BlockStateModelPart loadTieModel() {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TRACK_TIE);
	}

	private static BlockStateModelPart loadLeftSegmentModel() {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TRACK_SEGMENT_LEFT);
	}

	private static BlockStateModelPart loadRightSegmentModel() {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TRACK_SEGMENT_RIGHT);
	}

	private static BlockStateModelPart loadGirderTopModel() {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.GIRDER_SEGMENT_TOP);
	}

	private static BlockStateModelPart loadGirderMiddleModel() {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.GIRDER_SEGMENT_MIDDLE);
	}

	private static BlockStateModelPart loadGirderBottomModel() {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.GIRDER_SEGMENT_BOTTOM);
	}

	private static void submitPart(PoseStack root, Pose local, SubmitNodeCollector collector, BlockStateModelPart part,
		int light) {
		if (part == null)
			return;
		root.pushPose();
		root.last()
			.pose()
			.mul(local.pose());
		root.last()
			.normal()
			.mul(local.normal());
		collector.submitBlockModel(root, RenderTypes.cutoutMovingBlock(), List.of(part), BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		root.popPose();
	}

	private static void renderPart(PoseStack root, Pose local, VertexConsumer consumer, BlockStateModelPart part,
		int light) {
		if (part == null)
			return;
		PoseStack pose = compose(root, local);
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setColor(0xFFFFFFFF);
		quadInstance.setLightCoords(light);
		quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

		renderQuads(part.getQuads(null), pose, consumer, quadInstance);
		for (Direction side : Direction.values())
			renderQuads(part.getQuads(side), pose, consumer, quadInstance);
	}

	private static void renderPart(Pose root, Pose local, VertexConsumer consumer, BlockStateModelPart part, int light) {
		if (part == null)
			return;
		PoseStack pose = compose(root, local);
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setColor(0xFFFFFFFF);
		quadInstance.setLightCoords(light);
		quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

		renderQuads(part.getQuads(null), pose, consumer, quadInstance);
		for (Direction side : Direction.values())
			renderQuads(part.getQuads(side), pose, consumer, quadInstance);
	}

	private static void renderQuads(List<BakedQuad> quads, PoseStack pose, VertexConsumer consumer,
		QuadInstance quadInstance) {
		for (BakedQuad quad : quads)
			consumer.putBakedQuad(pose.last(), quad, quadInstance);
	}

	private static PoseStack compose(PoseStack root, Pose local) {
		PoseStack pose = new PoseStack();
		pose.last()
			.pose()
			.set(root.last()
				.pose())
			.mul(local.pose());
		pose.last()
			.normal()
			.set(root.last()
				.normal())
			.mul(local.normal());
		return pose;
	}

	private static PoseStack compose(Pose root, Pose local) {
		PoseStack pose = new PoseStack();
		pose.last()
			.pose()
			.set(root.pose())
			.mul(local.pose());
		pose.last()
			.normal()
			.set(root.normal())
			.mul(local.normal());
		return pose;
	}

	private static Level level(TrackBlockEntity be) {
		return be.getLevel();
	}

	private static class TrackRenderState extends BlockEntityRenderState {
		TrackBlockEntity blockEntity;
	}

	public static Vec3 getModelAngles(Vec3 normal, Vec3 diff) {
		double diffX = diff.x();
		double diffY = diff.y();
		double diffZ = diff.z();
		double len = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
		double yaw = Mth.atan2(diffX, diffZ);
		double pitch = Mth.atan2(len, diffY) - Math.PI * .5;

		Vec3 yawPitchNormal = VecHelper.rotate(VecHelper.rotate(new Vec3(0, 1, 0), AngleHelper.deg(pitch), Axis.X),
			AngleHelper.deg(yaw), Axis.Y);

		double signum = Math.signum(yawPitchNormal.dot(normal));
		if (Math.abs(signum) < 0.5f)
			signum = yawPitchNormal.distanceToSqr(normal) < 0.5f ? -1 : 1;
		double dot = diff.cross(normal)
			.normalize()
			.dot(yawPitchNormal);
		double roll = Math.acos(Mth.clamp(dot, -1, 1)) * signum;
		return new Vec3(pitch, yaw, roll);
	}

	public boolean shouldRenderOffScreen(TrackBlockEntity be) {
		return true;
	}

	public int getViewDistance() {
		return 192;
	}
}
