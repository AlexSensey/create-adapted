package com.simibubi.create.content.trains.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.BezierConnection;

import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TrackGraphVisualizer {

	@FunctionalInterface
	public interface SignalLineRenderer {
		void render(TrackEdge edge, double fromT, double toT, int color);
	}

	public static boolean forEachVisibleSignalEdgeGroup(TrackGraph graph, Vec3 camera, SignalLineRenderer renderer) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return false;
		AABB box = graph.getBounds(mc.level).box;
		if (box == null || !box.intersects(new AABB(camera, camera)
			.inflate(50)))
			return false;

		Map<UUID, SignalEdgeGroup> allGroups = Create.RAILWAYS.sided(null).signalEdgeGroups;
		boolean renderedAny = false;

		for (Entry<TrackNodeLocation, TrackNode> nodeEntry : graph.nodes.entrySet()) {
			TrackNodeLocation nodeLocation = nodeEntry.getKey();
			TrackNode node = nodeEntry.getValue();
			if (nodeLocation == null)
				continue;

			Vec3 location = nodeLocation.getLocation();
			if (location.distanceTo(camera) > 50)
				continue;
			if (!mc.level.dimension()
				.equals(nodeLocation.dimension))
				continue;

			Map<TrackNode, TrackEdge> map = graph.connectionsByNode.get(node);
			if (map == null)
				continue;

			int hashCode = node.hashCode();
			for (Entry<TrackNode, TrackEdge> entry : map.entrySet()) {
				TrackNode other = entry.getKey();
				TrackEdge edge = entry.getValue();
				EdgeData signalData = edge.getEdgeData();

				if (!edge.node1.location.dimension.equals(edge.node2.location.dimension))
					continue;
				if (other.hashCode() > hashCode && other.location.getLocation()
					.distanceTo(camera) <= 50)
					continue;

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
							if (group != null) {
								renderer.render(edge, previous + (previous == 0 ? 0 : 1 / 16d / length),
									boundaryT - 1 / 16d / length, group.color.get()
										.getRGB());
								renderedAny = true;
							}
							previous = boundaryT;
							previousBoundary = boundary;
						}

						if (previousBoundary != null) {
							SignalEdgeGroup group = allGroups.get(previousBoundary.getGroup(other));
							if (group != null) {
								renderer.render(edge, previous + 1 / 16d / length, 1, group.color.get()
									.getRGB());
								renderedAny = true;
							}
							continue;
						}
					}

					UUID singleGroup = signalData.getEffectiveEdgeGroupId(graph);
					SignalEdgeGroup singleEdgeGroup = singleGroup == null ? null : allGroups.get(singleGroup);
					if (singleEdgeGroup == null)
						continue;
					renderer.render(edge, 0, 1, singleEdgeGroup.color.get()
						.getRGB());
					renderedAny = true;
					continue;
				}

				if (signalData.hasSignalBoundaries()) {
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
						continue;
					UUID initialGroupId = currentBoundary.getGroup(node);
					if (initialGroupId == null)
						continue;
					SignalEdgeGroup initialGroup = allGroups.get(initialGroupId);
					if (initialGroup == null)
						continue;

					int currentColor = initialGroup.color.get()
						.getRGB();
					double previousT = 0;
					BezierConnection turn = edge.getTurn();

					for (int i = 1; i <= turn.getSegmentCount(); i++) {
						double currentT = i / (double) turn.getSegmentCount();
						double position = currentT * turn.getLength();

						if (currentBoundary != null && position > currentBoundaryPosition) {
							renderer.render(edge, previousT, (currentBoundaryPosition - 1 / 8d) / turn.getLength(),
								currentColor);
							renderedAny = true;

							previousT = (currentBoundaryPosition + 1 / 8d) / turn.getLength();
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

						renderer.render(edge, previousT, currentT, currentColor);
						renderedAny = true;
						previousT = currentT;
					}
					continue;
				}

				UUID singleGroup = signalData.getEffectiveEdgeGroupId(graph);
				SignalEdgeGroup singleEdgeGroup = singleGroup == null ? null : allGroups.get(singleGroup);
				if (singleEdgeGroup == null)
					continue;
				renderer.render(edge, 0, 1, singleEdgeGroup.color.get()
					.getRGB());
				renderedAny = true;
			}
		}

		return renderedAny;
	}

	public static void visualiseSignalEdgeGroups(TrackGraph graph) {
		Minecraft mc = Minecraft.getInstance();
		Entity cameraEntity = mc.getCameraEntity();
		if (cameraEntity == null || mc.level == null)
			return;
		AABB box = graph.getBounds(mc.level).box;
		if (box == null || !box.intersects(cameraEntity.getBoundingBox()
			.inflate(50)))
			return;

		Vec3 camera = cameraEntity.getEyePosition();
		Outliner outliner = Outliner.getInstance();
		Map<UUID, SignalEdgeGroup> allGroups = Create.RAILWAYS.sided(null).signalEdgeGroups;
		float width = 1 / 8f;

		for (Entry<TrackNodeLocation, TrackNode> nodeEntry : graph.nodes.entrySet()) {
			TrackNodeLocation nodeLocation = nodeEntry.getKey();
			TrackNode node = nodeEntry.getValue();
			if (nodeLocation == null)
				continue;

			Vec3 location = nodeLocation.getLocation();
			if (location.distanceTo(camera) > 50)
				continue;
			if (!mc.level.dimension()
				.equals(nodeLocation.dimension))
				continue;

			Map<TrackNode, TrackEdge> map = graph.connectionsByNode.get(node);
			if (map == null)
				continue;

			int hashCode = node.hashCode();
			for (Entry<TrackNode, TrackEdge> entry : map.entrySet()) {
				TrackNode other = entry.getKey();
				TrackEdge edge = entry.getValue();
				EdgeData signalData = edge.getEdgeData();

				if (!edge.node1.location.dimension.equals(edge.node2.location.dimension))
					continue;
				if (other.hashCode() > hashCode && other.location.getLocation()
					.distanceTo(camera) <= 50)
					continue;

				Vec3 yOffset = new Vec3(0, (other.hashCode() > hashCode ? 6 : 5) / 64f, 0);
				Vec3 startPoint = edge.getPosition(graph, 0);
				Vec3 endPoint = edge.getPosition(graph, 1);

				if (!edge.isTurn()) {
					if (signalData.hasSignalBoundaries()) {
						double prev = 0;
						double length = edge.getLength();
						SignalBoundary prevBoundary = null;
						SignalEdgeGroup group = null;

						for (TrackEdgePoint trackEdgePoint : signalData.getPoints()) {
							if (!(trackEdgePoint instanceof SignalBoundary boundary))
								continue;

							prevBoundary = boundary;
							group = allGroups.get(boundary.getGroup(node));

							if (group != null)
								outliner
									.showLine(Pair.of(boundary, edge),
										edge.getPosition(graph, prev + (prev == 0 ? 0 : 1 / 16f / length))
											.add(yOffset),
										edge.getPosition(graph,
											(prev = boundary.getLocationOn(edge) / length) - 1 / 16f / length)
											.add(yOffset))
									.colored(group.color.get())
									.lineWidth(width);
						}

						if (prevBoundary != null) {
							group = allGroups.get(prevBoundary.getGroup(other));
							if (group != null)
								outliner.showLine(edge, edge.getPosition(graph, prev + 1 / 16f / length)
									.add(yOffset), endPoint.add(yOffset))
									.colored(group.color.get())
									.lineWidth(width);
							continue;
						}
					}

					UUID singleGroup = signalData.getEffectiveEdgeGroupId(graph);
					SignalEdgeGroup singleEdgeGroup = singleGroup == null ? null : allGroups.get(singleGroup);
					if (singleEdgeGroup == null)
						continue;
					outliner.showLine(edge, startPoint.add(yOffset), endPoint.add(yOffset))
						.colored(singleEdgeGroup.color.get())
						.lineWidth(width);
					continue;
				}

				if (signalData.hasSignalBoundaries()) {
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
						continue;
					UUID initialGroupId = currentBoundary.getGroup(node);
					if (initialGroupId == null)
						continue;
					SignalEdgeGroup initialGroup = allGroups.get(initialGroupId);
					if (initialGroup == null)
						continue;

					Color currentColour = initialGroup.color.get();
					Vec3 previous = null;
					BezierConnection turn = edge.getTurn();

					for (int i = 0; i <= turn.getSegmentCount(); i++) {
						double f = i * 1f / turn.getSegmentCount();
						double position = f * turn.getLength();
						Vec3 current = edge.getPosition(graph, f);

						if (previous != null) {
							if (currentBoundary != null && position > currentBoundaryPosition) {
								current = edge.getPosition(graph, (currentBoundaryPosition - width) / turn.getLength());
								outliner
									.showLine(Pair.of(edge, previous), previous.add(yOffset), current.add(yOffset))
									.colored(currentColour)
									.lineWidth(width);
								current = edge.getPosition(graph, (currentBoundaryPosition + width) / turn.getLength());
								previous = current;
								UUID newId = currentBoundary.getGroup(other);
								if (newId != null && allGroups.containsKey(newId))
									currentColour = allGroups.get(newId).color.get();

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

							outliner.showLine(Pair.of(edge, previous), previous.add(yOffset), current.add(yOffset))
								.colored(currentColour)
								.lineWidth(width);
						}

						previous = current;
					}
					continue;
				}

				UUID singleGroup = signalData.getEffectiveEdgeGroupId(graph);
				SignalEdgeGroup singleEdgeGroup = singleGroup == null ? null : allGroups.get(singleGroup);
				if (singleEdgeGroup == null)
					continue;
				Vec3 previous = null;
				BezierConnection turn = edge.getTurn();
				for (int i = 0; i <= turn.getSegmentCount(); i++) {
					Vec3 current = edge.getPosition(graph, i * 1f / turn.getSegmentCount());
					if (previous != null)
						outliner.showLine(Pair.of(edge, previous), previous.add(yOffset), current.add(yOffset))
							.colored(singleEdgeGroup.color.get())
							.lineWidth(width);
					previous = current;
				}
			}
		}
	}

	public static void debugViewGraph(TrackGraph graph, boolean extended) {
	}
}
