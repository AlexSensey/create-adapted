package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraphVisualizer;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem.OverlapResult;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class TrackTargetingClient {

	public static final Identifier STATION_INDICATOR_TEXTURE = Create.asResource("textures/block/track_station_indicator.png");
	public static final Identifier SIGNAL_INDICATOR_TEXTURE = Create.asResource("textures/block/track_signal_indicator.png");
	public static final Identifier OBSERVER_INDICATOR_TEXTURE = Create.asResource("textures/block/observer_indicator.png");

	static BlockPos lastHovered;
	static boolean lastDirection;
	static EdgePointType<?> lastType;
	static BezierTrackPointLocation lastHoveredBezierSegment;

	static OverlapResult lastResult;
	static TrackGraphLocation lastLocation;

	public static void clientTick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			clear();
			return;
		}

		Vec3 lookAngle = player.getLookAngle();

		BlockPos hovered = null;
		boolean direction = false;
		EdgePointType<?> type = null;
		BezierTrackPointLocation hoveredBezier = null;

		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof TrackTargetingBlockItem ttbi)
			type = ttbi.getType(stack);

		boolean alreadySelected = stack.has(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);

		if (type != null) {
			BezierPointSelection bezierSelection = TrackBlockOutline.result;

			if (alreadySelected) {
				hovered = stack.get(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
				direction = stack.getOrDefault(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION, false);
				if (stack.has(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER))
					hoveredBezier = stack.get(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);

			} else if (bezierSelection != null) {
				hovered = bezierSelection.blockEntity()
					.getBlockPos();
				hoveredBezier = bezierSelection.loc();
				direction = lookAngle.dot(bezierSelection.direction()) < 0;

			} else {
				HitResult hitResult = mc.hitResult;
				if (hitResult != null && hitResult.getType() == Type.BLOCK) {
					BlockHitResult blockHitResult = (BlockHitResult) hitResult;
					BlockPos pos = blockHitResult.getBlockPos();
					BlockState blockState = mc.level.getBlockState(pos);
					if (blockState.getBlock() instanceof ITrackBlock track) {
						direction = track.getNearestTrackAxis(mc.level, pos, blockState, lookAngle)
							.getSecond() == AxisDirection.POSITIVE;
						hovered = pos;
					}
				}
			}
		}

		if (hovered == null) {
			clear();
			return;
		}

		BlockState hoveredState = mc.level.getBlockState(hovered);
		if (!(hoveredState.getBlock() instanceof ITrackBlock)) {
			clear();
			return;
		}

		lastType = type;
		lastHovered = hovered;
		lastDirection = direction;
		lastHoveredBezierSegment = hoveredBezier;
		lastResult = null;
		lastLocation = null;
		TrackTargetingBlockItem.withGraphLocation(mc.level, hovered, direction, hoveredBezier, type,
			(result, location) -> {
				lastResult = result;
				lastLocation = location;
			});

		// Rendering is done from the level render event so the marker behaves like the old track overlay.
	}

	public static void logOverlayTransform(RenderedTrackOverlayType type, BlockPos pos, AxisDirection direction,
		BezierTrackPointLocation bezier, TrackShape shape, Vec3 axis, Vec3 diff, Vec3 normal, Vec3 offset,
		Vec3 angles) {
	}

	public static void logInvalidOverlayTransform(RenderedTrackOverlayType type, BlockPos pos, AxisDirection direction,
		BezierTrackPointLocation bezier, String reason) {
	}

	public static void render(PoseStack ms, MultiBufferSource buffer, Vec3 camera) {
		if (lastHovered == null || lastResult == null)
			return;

		if (lastLocation == null || lastResult.feedback != null)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		BlockState trackState = mc.level.getBlockState(lastHovered);
		if (!(trackState.getBlock() instanceof ITrackBlock track)) {
			clear();
			return;
		}

		RenderedTrackOverlayType overlayType = getOverlayType(lastType);
		if (overlayType == null)
			return;

		Identifier texture = getIndicatorTexture(lastType);
		if (texture == null)
			return;

		VertexConsumer vb = buffer.getBuffer(RenderTypes.entityCutout(texture));
		ms.pushPose();
		ms.translate(lastHovered.getX() - camera.x, lastHovered.getY() - camera.y, lastHovered.getZ() - camera.z);
		PartialModel overlay = track instanceof TrackBlock trackBlock
			? trackBlock.prepareTrackOverlay(ms, mc.level, lastHovered, trackState, lastHoveredBezierSegment,
				lastDirection ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, overlayType)
			: track.prepareTrackOverlay(TransformStack.of(ms), mc.level, lastHovered, trackState,
				lastHoveredBezierSegment, lastDirection ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE,
				overlayType);
		if (overlay == null) {
			ms.popPose();
			return;
		}
		scaleTrackOverlayModel(ms);
		renderStationIndicatorCuboid(ms.last(), vb, 0, 0, 0, LightCoordsUtil.pack(15, 15));
		ms.popPose();
	}

	public static void submit(PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (lastHovered == null || lastResult == null)
			return;

		if (lastLocation == null || lastResult.feedback != null)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		BlockState trackState = mc.level.getBlockState(lastHovered);
		if (!(trackState.getBlock() instanceof ITrackBlock track)) {
			clear();
			return;
		}

		RenderedTrackOverlayType overlayType = getOverlayType(lastType);
		if (overlayType == null)
			return;

		StandaloneModelKey<BlockStateModelPart> key = getOverlayModel(lastType);
		if (key == null)
			return;

		BlockStateModelPart part = mc.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;

		Vec3 camera = cameraRenderState.pos;
		ms.pushPose();
		ms.translate(lastHovered.getX() - camera.x, lastHovered.getY() - camera.y, lastHovered.getZ() - camera.z);
		PartialModel overlay = track instanceof TrackBlock trackBlock
			? trackBlock.prepareTrackOverlay(ms, mc.level, lastHovered, trackState, lastHoveredBezierSegment,
				lastDirection ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, overlayType)
			: track.prepareTrackOverlay(TransformStack.of(ms), mc.level, lastHovered, trackState,
				lastHoveredBezierSegment, lastDirection ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE,
				overlayType);
		if (overlay == null) {
			ms.popPose();
			return;
		}
		scaleTrackOverlayModel(ms);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.pack(15, 15), 0, 0);
		ms.popPose();
		submitSignalPreviewLines(ms, collector, camera);
	}

	private static void submitSignalPreviewLines(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		if (lastType != EdgePointType.SIGNAL || lastLocation == null)
			return;

		Couple<TrackNode> nodes = lastLocation.edge.map(lastLocation.graph::locateNode);
		TrackEdge edge = lastLocation.graph.getConnection(nodes);
		if (edge == null)
			return;

		float halfWidth = 4 / 64f;
		float bottom = 10 / 64f;
		float top = 12 / 64f;

		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			renderSignalPreviewWalk(pose, consumer, camera, nodes, edge, halfWidth, bottom, top);
			TrackGraphVisualizer.forEachVisibleSignalEdgeGroup(lastLocation.graph, camera,
				(groupEdge, fromT, toT, color) -> renderSignalEdgeSection(pose, consumer, camera, groupEdge, fromT,
					toT, halfWidth, bottom, top, color));
		});
	}

	private static void renderSignalPreviewWalk(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		Couple<TrackNode> nodes, TrackEdge edge, float halfWidth, float bottom, float top) {
		double length = edge.getLength();
		if (length <= 0)
			return;

		renderSignalPreviewSide(pose, consumer, camera, nodes.getFirst(), nodes.getSecond(), lastLocation.position,
			halfWidth, bottom, top, 0xFF51C054);
		renderSignalPreviewSide(pose, consumer, camera, nodes.getSecond(), nodes.getFirst(),
			length - lastLocation.position, halfWidth, bottom, top, 0xFFEBC255);
	}

	private static void renderSignalPreviewSide(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackNode fromNode, TrackNode toNode, double startPosition, float halfWidth, float bottom, float top,
		int color) {
		TrackEdge startEdge = lastLocation.graph.getConnectionsFrom(fromNode)
			.get(toNode);
		if (startEdge == null || startEdge.getLength() <= 0)
			return;

		if (!renderPreviewUntilBoundary(pose, consumer, camera, startEdge, startPosition, halfWidth, bottom, top,
			color))
			return;

		Set<TrackEdge> visited = new HashSet<>();
		visited.add(startEdge);
		TrackEdge oppositeStart = lastLocation.graph.getConnectionsFrom(toNode)
			.get(fromNode);
		if (oppositeStart != null)
			visited.add(oppositeStart);

		List<Couple<TrackNode>> frontier = new ArrayList<>();
		frontier.add(Couple.create(toNode, fromNode));
		while (!frontier.isEmpty()) {
			Couple<TrackNode> couple = frontier.remove(0);
			TrackNode currentNode = couple.getFirst();
			TrackNode previousNode = couple.getSecond();

			for (Entry<TrackNode, TrackEdge> entry : lastLocation.graph.getConnectionsFrom(currentNode)
				.entrySet()) {
				TrackNode nextNode = entry.getKey();
				if (nextNode == previousNode)
					continue;

				TrackEdge edge = entry.getValue();
				if (!visited.add(edge))
					continue;

				TrackEdge opposite = lastLocation.graph.getConnectionsFrom(nextNode)
					.get(currentNode);
				if (opposite != null)
					visited.add(opposite);

				if (!renderPreviewUntilBoundary(pose, consumer, camera, edge, 0, halfWidth, bottom, top, color))
					continue;

				frontier.add(Couple.create(nextNode, currentNode));
			}
		}
	}

	private static boolean renderPreviewUntilBoundary(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackEdge edge, double startPosition, float halfWidth, float bottom, float top, int color) {
		double length = edge.getLength();
		if (length <= 0)
			return false;

		double startT = Math.max(0, Math.min(1, startPosition / length));
		SignalBoundary boundary = edge.getEdgeData()
			.next(EdgePointType.SIGNAL, startPosition);
		double endT = 1;
		boolean canContinue = true;
		if (boundary != null) {
			endT = Math.max(startT, boundary.getLocationOn(edge) / length - 1 / 16d / length);
			canContinue = false;
		}

		renderSignalEdgeSection(pose, consumer, camera, edge, startT, endT, halfWidth, bottom, top, color);
		return canContinue;
	}

	private static boolean renderSignalGroupEdge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackNode node,
		TrackNode other, TrackEdge edge, Map<UUID, SignalEdgeGroup> allGroups, float halfWidth, float bottom,
		float top) {
		if (!edge.node1.getLocation().dimension.equals(edge.node2.getLocation().dimension))
			return false;

		boolean renderedAny = false;
		EdgeData signalData = edge.getEdgeData();
		if (!edge.isTurn()) {
			if (signalData.hasSignalBoundaries()) {
				double previous = 0;
				double length = edge.getLength();
				SignalBoundary previousBoundary = null;

				for (TrackEdgePoint trackEdgePoint : signalData.getPoints()) {
					if (!(trackEdgePoint instanceof SignalBoundary boundary))
						continue;

					SignalEdgeGroup group = allGroups.get(boundary.getGroup(node));
					double boundaryT = boundary.getLocationOn(edge) / length;
					if (group != null)
						renderSignalEdgeSection(pose, consumer, camera, edge,
							previous + (previous == 0 ? 0 : 1 / 16d / length),
							boundaryT - 1 / 16d / length, halfWidth, bottom, top, group.color.get()
								.getRGB());
					renderedAny |= group != null;
					previous = boundaryT;
					previousBoundary = boundary;
				}

				if (previousBoundary != null) {
					SignalEdgeGroup group = allGroups.get(previousBoundary.getGroup(other));
					if (group != null)
						renderSignalEdgeSection(pose, consumer, camera, edge, previous + 1 / 16d / length, 1,
							halfWidth, bottom, top, group.color.get()
								.getRGB());
					return renderedAny || group != null;
				}
			}

			return renderSingleSignalGroupEdge(pose, consumer, camera, edge, allGroups, halfWidth, bottom, top);
		}

		if (signalData.hasSignalBoundaries())
			renderedAny |= renderSignalTurnWithBoundaries(pose, consumer, camera, node, other, edge, signalData, allGroups, halfWidth,
				bottom, top);

		return renderedAny || renderSingleSignalGroupEdge(pose, consumer, camera, edge, allGroups, halfWidth, bottom,
			top);
	}

	private static boolean renderSignalTurnWithBoundaries(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackNode node, TrackNode other, TrackEdge edge, EdgeData signalData, Map<UUID, SignalEdgeGroup> allGroups,
		float halfWidth, float bottom, float top) {
		Iterator<TrackEdgePoint> points = signalData.getPoints()
			.iterator();
		SignalBoundary currentBoundary = null;
		double currentBoundaryPosition = 0;
		while (points.hasNext()) {
			TrackEdgePoint next = points.next();
			if (!(next instanceof SignalBoundary signal))
				continue;
			currentBoundary = signal;
			currentBoundaryPosition = signal.getLocationOn(edge);
			break;
		}

		if (currentBoundary == null)
			return false;
		SignalEdgeGroup initialGroup = allGroups.get(currentBoundary.getGroup(node));
		if (initialGroup == null)
			return false;

		boolean renderedAny = false;
		int currentColor = initialGroup.color.get()
			.getRGB();
		double previousT = 0;
		BezierConnection turn = edge.getTurn();

		for (int i = 1; i <= turn.getSegmentCount(); i++) {
			double currentT = i / (double) turn.getSegmentCount();
			double position = currentT * turn.getLength();

			if (currentBoundary != null && position > currentBoundaryPosition) {
				double beforeBoundaryT = (currentBoundaryPosition - halfWidth) / turn.getLength();
				renderSignalEdgeSection(pose, consumer, camera, edge, previousT, beforeBoundaryT, halfWidth, bottom,
					top, currentColor);
				renderedAny = true;

				double afterBoundaryT = (currentBoundaryPosition + halfWidth) / turn.getLength();
				previousT = afterBoundaryT;
				UUID newId = currentBoundary.getGroup(other);
				if (newId != null && allGroups.containsKey(newId))
					currentColor = allGroups.get(newId).color.get()
						.getRGB();

				currentBoundary = null;
				while (points.hasNext()) {
					TrackEdgePoint next = points.next();
					if (!(next instanceof SignalBoundary signal))
						continue;
					currentBoundary = signal;
					currentBoundaryPosition = signal.getLocationOn(edge);
					break;
				}
			}

			renderSignalEdgeSection(pose, consumer, camera, edge, previousT, currentT, halfWidth, bottom, top,
				currentColor);
			renderedAny = true;
			previousT = currentT;
		}
		return renderedAny;
	}

	private static boolean renderSingleSignalGroupEdge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackEdge edge, Map<UUID, SignalEdgeGroup> allGroups, float halfWidth, float bottom, float top) {
		UUID singleGroup = edge.getEdgeData()
			.getEffectiveEdgeGroupId(lastLocation.graph);
		SignalEdgeGroup singleEdgeGroup = singleGroup == null ? null : allGroups.get(singleGroup);
		if (singleEdgeGroup == null)
			return false;
		renderSignalEdgeSection(pose, consumer, camera, edge, 0, 1, halfWidth, bottom, top, singleEdgeGroup.color.get()
			.getRGB());
		return true;
	}

	private static void renderSignalPreviewFallback(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackEdge edge, float halfWidth, float bottom, float top) {
		double length = edge.getLength();
		if (length <= 0)
			return;

		double signalT = Math.max(0, Math.min(1, lastLocation.position / length));
		double signalGap = Math.min(1 / 16d / length, .025d);
		renderSignalEdgeSection(pose, consumer, camera, edge, 0, Math.max(0, signalT - signalGap), halfWidth, bottom,
			top, 0xFFEBC255);
		renderSignalEdgeSection(pose, consumer, camera, edge, Math.min(1, signalT + signalGap), 1, halfWidth, bottom,
			top, 0xFF51C054);
	}

	private static void renderSignalEdgeSection(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		TrackEdge edge, double fromT, double toT, float halfWidth, float bottom, float top, int color) {
		fromT = Math.max(0, Math.min(1, fromT));
		toT = Math.max(0, Math.min(1, toT));
		if (Math.abs(toT - fromT) < 1e-5)
			return;
		if (toT < fromT) {
			double swap = fromT;
			fromT = toT;
			toT = swap;
		}

		int segments = edge.isTurn() ? Math.max(4, edge.getTurn()
			.getSegmentCount() * 4) : 1;
		List<Vec3> points = new ArrayList<>(segments + 1);
		for (int i = 0; i <= segments; i++) {
			double f = i / (double) segments;
			double t = fromT + (toT - fromT) * f;
			points.add(edge.getPosition(lastLocation.graph, t));
		}
		Vec3 offset = new Vec3(0, top, 0);
		for (int i = 0; i < points.size() - 1; i++)
			renderSignalPreviewStrip(pose, consumer, camera, points.get(i)
				.add(offset), points.get(i + 1)
					.add(offset), halfWidth, color);
	}

	public static void scaleTrackOverlayModel(PoseStack ms) {
		ms.translate(.5, 0, .5);
		ms.scale(1 + 1 / 16f, 1 + 1 / 16f, 1 + 1 / 16f);
		ms.translate(-.5, 0, -.5);
	}

	private static RenderedTrackOverlayType getOverlayType(EdgePointType<?> type) {
		if (type == EdgePointType.STATION)
			return RenderedTrackOverlayType.STATION;
		if (type == EdgePointType.SIGNAL)
			return RenderedTrackOverlayType.SIGNAL;
		if (type == EdgePointType.OBSERVER)
			return RenderedTrackOverlayType.OBSERVER;
		return null;
	}

	private static StandaloneModelKey<BlockStateModelPart> getOverlayModel(EdgePointType<?> type) {
		if (type == EdgePointType.STATION)
			return CreateStandaloneModels.TRACK_STATION_OVERLAY;
		if (type == EdgePointType.SIGNAL)
			return CreateStandaloneModels.TRACK_SIGNAL_OVERLAY;
		if (type == EdgePointType.OBSERVER)
			return CreateStandaloneModels.TRACK_OBSERVER_OVERLAY;
		return null;
	}

	private static Identifier getIndicatorTexture(EdgePointType<?> type) {
		if (type == EdgePointType.STATION)
			return STATION_INDICATOR_TEXTURE;
		if (type == EdgePointType.SIGNAL)
			return SIGNAL_INDICATOR_TEXTURE;
		if (type == EdgePointType.OBSERVER)
			return OBSERVER_INDICATOR_TEXTURE;
		return null;
	}

	public static void renderStationIndicator(PoseStack ms, MultiBufferSource buffer, Vec3 camera, BlockPos pos,
		int light) {
		VertexConsumer vb = buffer.getBuffer(RenderTypes.entityCutout(STATION_INDICATOR_TEXTURE));
		ms.pushPose();
		ms.translate(-camera.x, -camera.y, -camera.z);
		renderStationIndicatorCuboid(ms.last(), vb, pos.getX(), pos.getY(), pos.getZ(), light);
		ms.popPose();
	}

	public static void renderStationIndicatorCuboid(PoseStack.Pose pose, VertexConsumer vb, float x, float y, float z,
		int light) {
		float x0 = x + 1 / 16f;
		float x1 = x + 15 / 16f;
		float y0 = y + 1 / 16f;
		float y1 = y + 3 / 16f;
		float z0 = z + 1 / 16f;
		float z1 = z + 15 / 16f;

		quad(pose, vb, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 1 / 32f, 1 / 32f, 15 / 32f, 15 / 32f, light,
			0, 1, 0);
		quad(pose, vb, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 1 / 32f, 1 / 32f, 15 / 32f, 15 / 32f, light,
			0, -1, 0);
		quad(pose, vb, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 1 / 32f, 14 / 32f, 15 / 32f, 16 / 32f,
			light, 0, 0, -1);
		quad(pose, vb, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 1 / 32f, 14 / 32f, 15 / 32f, 16 / 32f,
			light, 0, 0, 1);
		quad(pose, vb, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, 1 / 32f, 14 / 32f, 15 / 32f, 16 / 32f,
			light, -1, 0, 0);
		quad(pose, vb, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1 / 32f, 14 / 32f, 15 / 32f, 16 / 32f,
			light, 1, 0, 0);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vb, float x0, float y0, float z0, float x1, float y1,
		float z1, float x2, float y2, float z2, float x3, float y3, float z3, float u0, float v0, float u1,
		float v1, int light, float normalX, float normalY, float normalZ) {
		vertex(pose, vb, x0, y0, z0, u0, v0, light, normalX, normalY, normalZ);
		vertex(pose, vb, x1, y1, z1, u1, v0, light, normalX, normalY, normalZ);
		vertex(pose, vb, x2, y2, z2, u1, v1, light, normalX, normalY, normalZ);
		vertex(pose, vb, x3, y3, z3, u0, v1, light, normalX, normalY, normalZ);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer vb, float x, float y, float z, float u, float v,
		int light, float normalX, float normalY, float normalZ) {
		vb.addVertex(pose, x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normalX, normalY, normalZ);
	}

	private static void renderSignalPreviewStrip(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera,
		Vec3 worldStart, Vec3 worldEnd, float width, int color) {
		Vec3 start = worldStart.subtract(camera);
		Vec3 end = worldEnd.subtract(camera);
		Vec3 direction = end.subtract(start);
		if (direction.lengthSqr() < 1e-5)
			return;

		Vec3 normal = direction.cross(new Vec3(0, 1, 0));
		if (normal.lengthSqr() < 1e-5)
			normal = new Vec3(1, 0, 0);
		normal = normal.normalize()
			.scale(width);

		renderPreviewQuad(pose, consumer, start.add(normal), end.add(normal), end.subtract(normal),
			start.subtract(normal), color);
	}

	private static void renderPreviewQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c,
		Vec3 d, int color) {
		addPreviewVertex(pose, consumer, a, color);
		addPreviewVertex(pose, consumer, b, color);
		addPreviewVertex(pose, consumer, c, color);
		addPreviewVertex(pose, consumer, d, color);
		addPreviewVertex(pose, consumer, d, color);
		addPreviewVertex(pose, consumer, c, color);
		addPreviewVertex(pose, consumer, b, color);
		addPreviewVertex(pose, consumer, a, color);
	}

	private static void addPreviewVertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 vertex, int color) {
		consumer.addVertex(pose, (float) vertex.x, (float) vertex.y, (float) vertex.z)
			.setColor(color);
	}

	public static void clear() {
		lastHovered = null;
		lastResult = null;
		lastLocation = null;
		lastHoveredBezierSegment = null;
	}
}
