package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.glue.SuperGlueRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SelectionBoxRenderer {

	public static void renderSolidCuboid(PoseStack.Pose pose, VertexConsumer consumer, AABB box, int color) {
		float x1 = (float) box.minX;
		float y1 = (float) box.minY;
		float z1 = (float) box.minZ;
		float x2 = (float) box.maxX;
		float y2 = (float) box.maxY;
		float z2 = (float) box.maxZ;
		quadXZ(pose, consumer, y1, x1, z1, x2, z2, color);
		quadXZ(pose, consumer, y2, x1, z1, x2, z2, color);
		quadXY(pose, consumer, z1, x1, y1, x2, y2, color);
		quadXY(pose, consumer, z2, x1, y1, x2, y2, color);
		quadYZ(pose, consumer, x1, y1, z1, y2, z2, color);
		quadYZ(pose, consumer, x2, y1, z1, y2, z2, color);
	}

	public static void submit(PoseStack ms, SubmitNodeCollector collector, Vec3 camera, AABB worldBox, int color) {
		if (worldBox == null || worldBox.getSize() <= 0)
			return;

		AABB cameraRelative = worldBox.move(-camera.x, -camera.y, -camera.z);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> SuperGlueRenderer.renderWireframe(pose, consumer, cameraRelative, color, 235, 1 / 16f));
	}

	public static void submitSideEdges(PoseStack ms, SubmitNodeCollector collector, Vec3 camera,
		AABB worldBox, Axis depthAxis, int color) {
		if (worldBox == null || depthAxis == null)
			return;
		AABB cameraRelative = worldBox.move(-camera.x, -camera.y, -camera.z);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> SuperGlueRenderer.renderSideEdges(pose, consumer, cameraRelative,
				depthAxis, color, 235, 1 / 16f));
	}

	public static void submitFlatFrame(PoseStack ms, SubmitNodeCollector collector, Vec3 camera,
		AABB worldBox, Direction outward, int color, float thickness) {
		if (worldBox == null || outward == null)
			return;
		Vec3 offset = Vec3.atLowerCornerOf(outward.getUnitVec3i()).scale(1 / 32f + 1 / 512f);
		AABB box = worldBox.move(offset).move(-camera.x, -camera.y, -camera.z);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderFlatFrame(pose, consumer, box, outward.getAxis(), color, thickness));
	}

	public static void submitExtrudedFrame(PoseStack ms, SubmitNodeCollector collector, Vec3 camera,
		AABB worldBox, Direction outward, int color, float thickness) {
		if (worldBox == null || outward == null)
			return;

		Axis depthAxis = outward.getAxis();
		Vec3 frontOffset = Vec3.atLowerCornerOf(outward.getUnitVec3i()).scale(1 / 32f + 1 / 512f);
		AABB box = worldBox.move(frontOffset);
		box = box.inflate(depthAxis == Axis.X ? 0 : thickness,
			depthAxis == Axis.Y ? 0 : thickness,
			depthAxis == Axis.Z ? 0 : thickness);
		Vec3 depth = Vec3.atLowerCornerOf(outward.getOpposite().getUnitVec3i()).scale(1 / 16f);
		AABB cameraRelative = box.expandTowards(depth).move(-camera.x, -camera.y, -camera.z);

		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> SuperGlueRenderer.renderWireframe(pose, consumer, cameraRelative,
				color & 0xFFFFFF, 235, thickness));
	}

	private static void renderFlatFrame(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
		AABB box, Axis axis, int color, float thickness) {
		float x1 = (float) box.minX;
		float y1 = (float) box.minY;
		float z1 = (float) box.minZ;
		float x2 = (float) box.maxX;
		float y2 = (float) box.maxY;
		float z2 = (float) box.maxZ;
		float t = thickness;
		switch (axis) {
			case X -> {
				quadYZ(pose, consumer, x1, y1, z1, y1 + t, z2, color);
				quadYZ(pose, consumer, x1, y2 - t, z1, y2, z2, color);
				quadYZ(pose, consumer, x1, y1 + t, z1, y2 - t, z1 + t, color);
				quadYZ(pose, consumer, x1, y1 + t, z2 - t, y2 - t, z2, color);
			}
			case Y -> {
				quadXZ(pose, consumer, y1, x1, z1, x1 + t, z2, color);
				quadXZ(pose, consumer, y1, x2 - t, z1, x2, z2, color);
				quadXZ(pose, consumer, y1, x1 + t, z1, x2 - t, z1 + t, color);
				quadXZ(pose, consumer, y1, x1 + t, z2 - t, x2 - t, z2, color);
			}
			case Z -> {
				quadXY(pose, consumer, z1, x1, y1, x1 + t, y2, color);
				quadXY(pose, consumer, z1, x2 - t, y1, x2, y2, color);
				quadXY(pose, consumer, z1, x1 + t, y1, x2 - t, y1 + t, color);
				quadXY(pose, consumer, z1, x1 + t, y2 - t, x2 - t, y2, color);
			}
		}
	}

	private static void quadYZ(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
		float x, float y1, float z1, float y2, float z2, int color) {
		consumer.addVertex(pose, x, y1, z1).setColor(color);
		consumer.addVertex(pose, x, y2, z1).setColor(color);
		consumer.addVertex(pose, x, y2, z2).setColor(color);
		consumer.addVertex(pose, x, y1, z2).setColor(color);
		consumer.addVertex(pose, x, y1, z2).setColor(color);
		consumer.addVertex(pose, x, y2, z2).setColor(color);
		consumer.addVertex(pose, x, y2, z1).setColor(color);
		consumer.addVertex(pose, x, y1, z1).setColor(color);
	}

	private static void quadXZ(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
		float y, float x1, float z1, float x2, float z2, int color) {
		consumer.addVertex(pose, x1, y, z1).setColor(color);
		consumer.addVertex(pose, x1, y, z2).setColor(color);
		consumer.addVertex(pose, x2, y, z2).setColor(color);
		consumer.addVertex(pose, x2, y, z1).setColor(color);
		consumer.addVertex(pose, x2, y, z1).setColor(color);
		consumer.addVertex(pose, x2, y, z2).setColor(color);
		consumer.addVertex(pose, x1, y, z2).setColor(color);
		consumer.addVertex(pose, x1, y, z1).setColor(color);
	}

	private static void quadXY(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
		float z, float x1, float y1, float x2, float y2, int color) {
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x2, y1, z).setColor(color);
		consumer.addVertex(pose, x2, y2, z).setColor(color);
		consumer.addVertex(pose, x1, y2, z).setColor(color);
		consumer.addVertex(pose, x1, y2, z).setColor(color);
		consumer.addVertex(pose, x2, y2, z).setColor(color);
		consumer.addVertex(pose, x2, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
	}
}
