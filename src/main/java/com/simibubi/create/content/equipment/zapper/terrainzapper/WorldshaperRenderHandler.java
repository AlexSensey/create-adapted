package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.render.SelectionBoxRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class WorldshaperRenderHandler {

	private static Collection<BlockPos> renderedPositions;
	private static List<PreviewEdge> previewEdges = List.of();
	private static Object selectionKey;

	public static void tick() {
		gatherSelectedBlocks();
	}

	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
		if (previewEdges.isEmpty())
			return;
		collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
			for (PreviewEdge edge : previewEdges) {
				double t = 1 / 48d;
				double x1 = edge.pos.getX() - camera.x - t;
				double y1 = edge.pos.getY() - camera.y - t;
				double z1 = edge.pos.getZ() - camera.z - t;
				double x2 = edge.pos.getX() - camera.x + t;
				double y2 = edge.pos.getY() - camera.y + t;
				double z2 = edge.pos.getZ() - camera.z + t;
				switch (edge.axis) {
					case X -> x2 += 1;
					case Y -> y2 += 1;
					case Z -> z2 += 1;
				}
				SelectionBoxRenderer.renderSolidCuboid(pose, consumer,
					new AABB(x1, y1, z1, x2, y2, z2), 0xD0BFBFBF);
			}
		});
	}

	protected static void gatherSelectedBlocks() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || player.level() == null) {
			clearOutline();
			return;
		}
		ItemStack heldMain = player.getMainHandItem();
		ItemStack heldOff = player.getOffhandItem();
		boolean zapperInMain = AllItems.WORLDSHAPER.isIn(heldMain);
		boolean zapperInOff = AllItems.WORLDSHAPER.isIn(heldOff);

		if (zapperInMain && (!heldMain.has(AllDataComponents.SHAPER_SWAP) || !zapperInOff)) {
			createBrushOutline(player, heldMain);
			return;
		}
		if (zapperInOff) {
			createBrushOutline(player, heldOff);
			return;
		}
		clearOutline();
	}

	public static void createBrushOutline(LocalPlayer player, ItemStack zapper) {
		if (!zapper.has(AllDataComponents.SHAPER_BRUSH_PARAMS)) {
			clearOutline();
			return;
		}

		TerrainBrushes brushType = zapper.getOrDefault(AllDataComponents.SHAPER_BRUSH, TerrainBrushes.Cuboid);
		Brush brush = brushType.get();
		PlacementOptions placement = zapper.getOrDefault(AllDataComponents.SHAPER_PLACEMENT_OPTIONS, PlacementOptions.Merged);
		TerrainTools tool = zapper.getOrDefault(AllDataComponents.SHAPER_TOOL, TerrainTools.Fill);
		BlockPos params = zapper.get(AllDataComponents.SHAPER_BRUSH_PARAMS);
		brush.set(params.getX(), params.getY(), params.getZ());

		Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
		Vec3 range = player.getLookAngle().scale(128);
		BlockHitResult raytrace = player.level()
			.clip(new ClipContext(start, start.add(range), Block.OUTLINE, Fluid.NONE, player));
		if (raytrace == null || raytrace.getType() == Type.MISS) {
			clearOutline();
			return;
		}

		BlockPos pos = raytrace.getBlockPos()
			.offset(brush.getOffset(player.getLookAngle(), raytrace.getDirection(), placement));
		Object newSelectionKey = List.of(player.level(), pos.immutable(), raytrace.getDirection(), brushType,
			params.immutable(), placement, tool);
		if (newSelectionKey.equals(selectionKey))
			return;

		selectionKey = newSelectionKey;
		renderedPositions = brush.addToGlobalPositions(player.level(), pos, raytrace.getDirection(),
			new ArrayList<>(), tool);
		previewEdges = createOuterEdges(renderedPositions);
	}

	private static List<PreviewEdge> createOuterEdges(Collection<BlockPos> positions) {
		Set<PreviewEdge> edges = new HashSet<>();
		for (BlockPos pos : positions) {
			for (int y = 0; y <= 1; y++)
				for (int z = 0; z <= 1; z++)
					toggle(edges, new PreviewEdge(Axis.X, pos.offset(0, y, z)));
			for (int x = 0; x <= 1; x++)
				for (int z = 0; z <= 1; z++)
					toggle(edges, new PreviewEdge(Axis.Y, pos.offset(x, 0, z)));
			for (int x = 0; x <= 1; x++)
				for (int y = 0; y <= 1; y++)
					toggle(edges, new PreviewEdge(Axis.Z, pos.offset(x, y, 0)));
		}
		return new ArrayList<>(edges);
	}

	private static void toggle(Set<PreviewEdge> edges, PreviewEdge edge) {
		if (!edges.remove(edge))
			edges.add(edge);
	}

	private static void clearOutline() {
		renderedPositions = null;
		previewEdges = List.of();
		selectionKey = null;
	}

	private record PreviewEdge(Axis axis, BlockPos pos) {}
}
