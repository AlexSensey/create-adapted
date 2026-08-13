package com.simibubi.create.content.trains.track;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.utility.RaycastHelper;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.data.WorldAttached;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

@EventBusSubscriber(Dist.CLIENT)
public class TrackBlockOutline {

	public static WorldAttached<Map<BlockPos, TrackBlockEntity>> TRACKS_WITH_TURNS =
		new WorldAttached<>(w -> new HashMap<>());

	public static BezierPointSelection result;

	public static void pickCurves() {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.getCameraEntity() instanceof LocalPlayer player))
			return;
		if (mc.level == null)
			return;

		Vec3 origin = player.getEyePosition(AnimationTickHolder.getPartialTicks());
		double maxRange = mc.hitResult == null ? Double.MAX_VALUE
			: mc.hitResult.getLocation()
				.distanceToSqr(origin);

		result = null;

		double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
		Vec3 target = RaycastHelper.getTraceTarget(player, Math.min(maxRange, range) + 1, origin);
		Map<BlockPos, TrackBlockEntity> turns = TRACKS_WITH_TURNS.get(mc.level);

		pickFromTurns(origin, target, maxRange, turns);

		if (result == null)
			return;

		if (mc.hitResult != null && mc.hitResult.getType() != Type.MISS) {
			Vec3 priorLoc = mc.hitResult.getLocation();
			mc.hitResult = BlockHitResult.miss(priorLoc, Direction.UP, BlockPos.containing(priorLoc));
		}
	}

	private static void pickFromTurns(Vec3 origin, Vec3 target, double maxRange,
		Map<BlockPos, TrackBlockEntity> turns) {
		for (TrackBlockEntity be : turns.values()) {
			if (be == null || be.isRemoved())
				continue;
			for (BezierConnection bc : be.connections.values()) {
				if (!bc.isPrimary())
					continue;

				AABB bounds = bc.getBounds();
				if (!bounds.contains(origin) && bounds.clip(origin, target)
					.isEmpty())
					continue;

				float[] stepLUT = bc.getStepLUT();
				int segments = (int) (bc.getLength() * 2);
				AABB segmentBounds = AllShapes.TRACK_ORTHO.get(Direction.SOUTH)
					.bounds();
				segmentBounds = segmentBounds.move(-.5, segmentBounds.getYsize() / -2, -.5);

				int bestSegment = -1;
				double bestDistance = Double.MAX_VALUE;
				double newMaxRange = maxRange;

				for (int i = 0; i < stepLUT.length - 2; i++) {
					float t = stepLUT[i] * i / segments;
					float t1 = stepLUT[i + 1] * (i + 1) / segments;
					float t2 = stepLUT[i + 2] * (i + 2) / segments;

					Vec3 v1 = bc.getPosition(t);
					Vec3 v2 = bc.getPosition(t2);
					Vec3 diff = v2.subtract(v1);
					Vec3 angles = TrackRenderer.getModelAngles(bc.getNormal(t1), diff);

					Vec3 anchor = v1.add(diff.scale(.5));
					Vec3 localOrigin = origin.subtract(anchor);
					Vec3 localDirection = target.subtract(origin);
					localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.x), Axis.X);
					localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.y), Axis.Y);
					localDirection = VecHelper.rotate(localDirection, AngleHelper.deg(-angles.x), Axis.X);
					localDirection = VecHelper.rotate(localDirection, AngleHelper.deg(-angles.y), Axis.Y);

					Optional<Vec3> clip = segmentBounds.clip(localOrigin, localOrigin.add(localDirection));
					if (clip.isEmpty())
						continue;

					if (bestSegment != -1 && bestDistance < clip.get()
						.distanceToSqr(0, 0.25f, 0))
						continue;

					double distanceToSqr = clip.get()
						.distanceToSqr(localOrigin);
					if (distanceToSqr > maxRange)
						continue;

					bestSegment = i;
					newMaxRange = distanceToSqr;
					bestDistance = clip.get()
						.distanceToSqr(0, 0.25f, 0);

					BezierTrackPointLocation location = new BezierTrackPointLocation(bc.getKey(), i);
					result = new BezierPointSelection(be, location, anchor, angles, diff.normalize());
				}

				if (bestSegment != -1)
					maxRange = newMaxRange;
			}
		}
	}

	@SubscribeEvent
	public static void extractDiagonalTrackSelection(ExtractBlockOutlineRenderStateEvent event) {
		BlockState blockState = event.getBlockState();
		if (!(blockState.getBlock() instanceof TrackBlock))
			return;

		TrackShape shape = blockState.getValue(TrackBlock.SHAPE);
		if (shape != TrackShape.PD && shape != TrackShape.ND)
			return;

		BlockPos pos = event.getBlockPos();
		if (!event.getLevel()
			.getWorldBorder()
			.isWithinBounds(pos))
			return;

		float yRot = shape == TrackShape.PD ? 45 : -45;

		event.addCustomRenderer((renderState, submitNodeCollector, poseStack, levelRenderState) -> {
			renderStraightTrackSelection(pos, yRot, submitNodeCollector, poseStack, levelRenderState);
			return true;
		});
	}

	private static void renderStraightTrackSelection(BlockPos pos, float yRot, SubmitNodeCollector submitNodeCollector,
		PoseStack poseStack, LevelRenderState levelRenderState) {
		Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
		Vec3 blockAnchor = Vec3.atLowerCornerOf(pos)
			.subtract(cameraPos);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> LONG_ORTHO
			.forAllEdges((x1, y1, z1, x2, y2, z2) -> renderBlockOutlineEdge(pose, consumer, blockAnchor, yRot,
				new Vec3(x1, y1, z1), new Vec3(x2, y2, z2))));
	}

	public static void submitCurveSelection(PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR || mc.player == null)
			return;

		BezierPointSelection result = TrackBlockOutline.result;
		if (result == null)
			return;
		if (result.blockEntity() == null || result.blockEntity()
			.isRemoved())
			return;
		if (!result.blockEntity()
			.getConnections()
			.containsKey(result.loc()
				.curveTarget()))
			return;

		Vec3 camera = cameraRenderState.pos;
		if (result.vec()
			.distanceToSqr(camera) < 1 / 16f)
			return;
		double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1;
		if (result.vec()
			.distanceToSqr(camera) > range * range)
			return;

		Vec3 vec = result.vec()
			.subtract(camera);
		Vec3 angles = result.angles();

		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> AllShapes.TRACK_ORTHO
			.get(Direction.SOUTH)
			.forAllEdges((x1, y1, z1, x2, y2, z2) -> renderCurveOutlineEdge(pose, consumer, vec, angles,
				new Vec3(x1, y1, z1), new Vec3(x2, y2, z2))));
	}

	private static void renderCurveOutlineEdge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 anchor, Vec3 angles,
		Vec3 localStart, Vec3 localEnd) {
		Vec3 start = transformCurveOutlinePoint(localStart, anchor, angles);
		Vec3 end = transformCurveOutlinePoint(localEnd, anchor, angles);
		renderOutlineEdge(pose, consumer, start, end);
	}

	private static void renderBlockOutlineEdge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 anchor,
		float yRotDegrees, Vec3 localStart, Vec3 localEnd) {
		Vec3 start = transformBlockOutlinePoint(localStart, anchor, yRotDegrees);
		Vec3 end = transformBlockOutlinePoint(localEnd, anchor, yRotDegrees);
		renderOutlineEdge(pose, consumer, start, end);
	}

	private static void renderOutlineEdge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end) {
		Vec3 edge = end.subtract(start);
		if (edge.lengthSqr() < 1e-6)
			return;

		Vec3 view = start.add(end)
			.scale(-.5);
		Vec3 normal = edge.cross(view);
		if (normal.lengthSqr() < 1e-6)
			normal = edge.cross(new Vec3(0, 1, 0));
		if (normal.lengthSqr() < 1e-6)
			normal = new Vec3(1, 0, 0);
		normal = normal.normalize()
			.scale(1 / 256f);

		renderCurveOutlineQuad(pose, consumer, start.add(normal), end.add(normal), end.subtract(normal),
			start.subtract(normal));
	}

	private static Vec3 transformCurveOutlinePoint(Vec3 local, Vec3 anchor, Vec3 angles) {
		Vec3 vec = local.add(-.5, -.125f, -.5);
		vec = VecHelper.rotate(vec, AngleHelper.deg(angles.x), Axis.X);
		vec = VecHelper.rotate(vec, AngleHelper.deg(angles.y), Axis.Y);
		return vec.add(anchor)
			.add(0, .125f, 0);
	}

	private static Vec3 transformBlockOutlinePoint(Vec3 local, Vec3 anchor, float yRotDegrees) {
		Vec3 vec = local.add(-.5, -.5, -.5);
		vec = VecHelper.rotate(vec, yRotDegrees, Axis.Y);
		return vec.add(.5, .5, .5)
			.add(anchor);
	}

	private static void renderCurveOutlineQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c,
		Vec3 d) {
		addCurveOutlineVertex(pose, consumer, a);
		addCurveOutlineVertex(pose, consumer, b);
		addCurveOutlineVertex(pose, consumer, c);
		addCurveOutlineVertex(pose, consumer, d);
		addCurveOutlineVertex(pose, consumer, d);
		addCurveOutlineVertex(pose, consumer, c);
		addCurveOutlineVertex(pose, consumer, b);
		addCurveOutlineVertex(pose, consumer, a);
	}

	private static void addCurveOutlineVertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 vertex) {
		consumer.addVertex(pose, (float) vertex.x, (float) vertex.y, (float) vertex.z)
			.setColor(0x66000000);
	}

	public static void renderShape(VoxelShape s, PoseStack ms, VertexConsumer vb, Boolean valid) {
		PoseStack.Pose transform = ms.last();
		s.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
			float xDiff = (float) (x2 - x1);
			float yDiff = (float) (y2 - y1);
			float zDiff = (float) (z2 - z1);
			float length = Mth.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

			xDiff /= length;
			yDiff /= length;
			zDiff /= length;

			float r = 0f;
			float g = 0f;
			float b = 0f;

			if (valid != null && valid) {
				g = 1f;
				b = 1f;
				r = 1f;
			}

			if (valid != null && !valid) {
				r = 1f;
				b = 0.125f;
				g = 0.25f;
			}

			vb.addVertex(transform.pose(), (float) x1, (float) y1, (float) z1)
				.setColor(r, g, b, .4f)
				.setNormal(transform.copy(), xDiff, yDiff, zDiff)
				.setLineWidth(1);
			vb.addVertex(transform.pose(), (float) x2, (float) y2, (float) z2)
				.setColor(r, g, b, .4f)
				.setNormal(transform.copy(), xDiff, yDiff, zDiff)
				.setLineWidth(1);
		});
	}

	private static final VoxelShape LONG_CROSS =
		Shapes.or(TrackVoxelShapes.longOrthogonalZ(), TrackVoxelShapes.longOrthogonalX());
	private static final VoxelShape LONG_ORTHO = TrackVoxelShapes.longOrthogonalZ();
	private static final VoxelShape LONG_ORTHO_OFFSET = TrackVoxelShapes.longOrthogonalZOffset();

	private static void walkShapes(TrackShape shape, TransformStack<?> msr, Consumer<VoxelShape> renderer) {
		float angle45 = Mth.PI / 4;

		if (shape == TrackShape.XO || shape == TrackShape.CR_NDX || shape == TrackShape.CR_PDX)
			renderer.accept(AllShapes.TRACK_ORTHO.get(Direction.EAST));
		else if (shape == TrackShape.ZO || shape == TrackShape.CR_NDZ || shape == TrackShape.CR_PDZ)
			renderer.accept(AllShapes.TRACK_ORTHO.get(Direction.SOUTH));

		if (shape.isPortal()) {
			for (Direction d : Iterate.horizontalDirections) {
				if (TrackShape.asPortal(d) != shape)
					continue;
				msr.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(d)), Direction.UP);
				renderer.accept(LONG_ORTHO_OFFSET);
				return;
			}
		}

		if (shape == TrackShape.PD || shape == TrackShape.CR_PDX || shape == TrackShape.CR_PDZ) {
			msr.rotateCentered(angle45, Direction.UP);
			renderer.accept(LONG_ORTHO);
		} else if (shape == TrackShape.ND || shape == TrackShape.CR_NDX || shape == TrackShape.CR_NDZ) {
			msr.rotateCentered(-Mth.PI / 4, Direction.UP);
			renderer.accept(LONG_ORTHO);
		}

		if (shape == TrackShape.CR_O)
			renderer.accept(AllShapes.TRACK_CROSS);
		else if (shape == TrackShape.CR_D) {
			msr.rotateCentered(angle45, Direction.UP);
			renderer.accept(LONG_CROSS);
		}

		if (!(shape == TrackShape.AE || shape == TrackShape.AN || shape == TrackShape.AW || shape == TrackShape.AS))
			return;

		msr.translate(0, 1, 0);
		msr.rotateCentered(Mth.PI - AngleHelper.rad(shape.getModelRotation()), Direction.UP);
		msr.rotateX(angle45);
		msr.translate(0, -3 / 16f, 1 / 16f);
		renderer.accept(LONG_ORTHO);
	}

	public static record BezierPointSelection(TrackBlockEntity blockEntity, BezierTrackPointLocation loc, Vec3 vec,
		Vec3 angles, Vec3 direction) {
	}

}
