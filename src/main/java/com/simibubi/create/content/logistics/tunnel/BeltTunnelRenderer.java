package com.simibubi.create.content.logistics.tunnel;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity.SelectionMode;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BeltTunnelRenderer extends SmartBlockEntityRenderer<BeltTunnelBlockEntity> {

	private List<BlockStateModelPart> flapModel;
	private static final String[][] TUNNEL_MODE_PIXELS = {
		{
			"................",
			"................",
			"................",
			".####.####.####.",
			".####.####.####.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			"................",
			"..##...##...##..",
			"................",
			"..##...##...##..",
			"................",
			"................" },
		{
			"................",
			"....##..........",
			"....##..........",
			".##.##.###.####.",
			".##....###.####.",
			".#..##...#.#..#.",
			".#.......#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			"................",
			"..##...##...##..",
			"................",
			"..##...##...##..",
			"................",
			"................" },
		{
			"................",
			"................",
			"................",
			".####.####.####.",
			".####.####.####.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			"..##............",
			"..##...##.......",
			".......##...##..",
			"............##..",
			"................",
			"................" },
		{
			"................",
			"....##..........",
			"....##..........",
			".##.##.###.####.",
			".##....###.####.",
			".#..##...#.#..#.",
			".#.......#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			"..##............",
			"..##...##.......",
			".......##...##..",
			"............##..",
			"................",
			"................" },
		{
			"................",
			"................",
			"................",
			".####.####.####.",
			".####.####.####.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			"..##............",
			"..##............",
			"................",
			"..##............",
			"..##............",
			"................" },
		{
			"................",
			"................",
			"................",
			".####.####.####.",
			".####.####.####.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			"..##............",
			"..##........##..",
			".......##...##..",
			".......##.......",
			"................",
			"................" },
		{
			"................",
			"................",
			"................",
			".####.####.####.",
			".####.####.####.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			".#..#.#..#.#..#.",
			"................",
			".##############.",
			"................",
			"..##...##...##..",
			"..##...##...##..",
			"................",
			"................" } };

	public BeltTunnelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BeltTunnelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new TunnelRenderState();
	}

	@Override
	public void extractRenderState(BeltTunnelBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof TunnelRenderState tunnelState) {
			tunnelState.blockEntity = be;
			tunnelState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof TunnelRenderState tunnelState))
			return;
		BeltTunnelBlockEntity be = tunnelState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		renderFilters(be, ms, collector, state.lightCoords);
		renderTunnelMode(be, ms, collector);
		List<BlockStateModelPart> flap = getFlapModel();
		if (flap.isEmpty())
			return;

		for (Direction direction : Direction.values()) {
			if (!be.flaps.containsKey(direction))
				continue;
			float flapness = be.flaps.get(direction)
				.getValue(tunnelState.partialTicks);
			renderFlaps(ms, collector, flap, direction, flapness, state.lightCoords);
		}
	}

	private static void renderTunnelMode(BeltTunnelBlockEntity be, PoseStack ms, SubmitNodeCollector collector) {
		if (!(be instanceof BrassTunnelBlockEntity brass))
			return;
		if (brass.selectionMode == null || !brass.selectionMode.isActive())
			return;

		BlockState state = be.getBlockState();
		BrassTunnelModeSlot slot = (BrassTunnelModeSlot) new BrassTunnelModeSlot().fromSide(Direction.UP);
		if (!slot.shouldRender(be.getLevel(), be.getBlockPos(), state))
			return;

		Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), state);
		if (offset == null)
			return;
		if (!shouldRenderModeOverlay(be, slot, state))
			return;

		Direction side = Direction.UP;
		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateFilterSlot(ms, state, side);

		int color = slot.getOverrideColor();
		int iconColor = 0xFF000000 | color;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), BeltTunnelRenderer::renderModeFrame);

		SelectionMode mode = brass.selectionMode.get();
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderTunnelModeIcon(pose, consumer, mode, iconColor));
		ms.popPose();
	}

	private static boolean shouldRenderModeOverlay(BeltTunnelBlockEntity be, BrassTunnelModeSlot slot,
		BlockState state) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return false;
		if (!blockHit.getBlockPos()
			.equals(be.getBlockPos()))
			return false;
		if (blockHit.getDirection() != Direction.UP)
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
		return slot.testHit(be.getLevel(), be.getBlockPos(), state, localHit);
	}

	private static void renderModeFrame(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderTunnelModeIcon(Pose pose, VertexConsumer consumer, SelectionMode mode, int color) {
		String[] pixels = TUNNEL_MODE_PIXELS[mode.ordinal()];
		for (int y = 0; y < pixels.length; y++) {
			String row = pixels[y];
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) == '#')
					flatScaledPixelXY(pose, consumer, x, 15 - y, .22f, 1 / 512f, color);
			}
		}
	}

	private static void renderFilters(BeltTunnelBlockEntity be, PoseStack ms, SubmitNodeCollector collector, int light) {
		if (!(be instanceof BrassTunnelBlockEntity brass))
			return;
		SidedFilteringBehaviour filtering = brass.filtering;
		if (filtering == null || !filtering.isActive())
			return;

		BlockState state = be.getBlockState();
		for (Direction side : Direction.values()) {
			FilteringBehaviour sideFilter = filtering.get(side);
			if (sideFilter == null)
				continue;

			BrassTunnelFilterSlot slot = (BrassTunnelFilterSlot) new BrassTunnelFilterSlot().fromSide(side);
			if (!slot.shouldRender(be.getLevel(), be.getBlockPos(), state))
				continue;

			Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), state);
			if (offset == null)
				continue;
			Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
			ItemStack filter = filtering.getFilter(side);
			boolean hasFilter = !filter.isEmpty();
			boolean active = shouldRenderFilterOverlay(be, state, slot, side, offset);

			if (!hasFilter && active)
				renderFilterOverlay(ms, collector, offset, normal, side, false);

			if (!hasFilter)
				continue;

			ms.pushPose();
			ms.translate(offset.x + normal.x / 32d, offset.y + normal.y / 32d, offset.z + normal.z / 32d);
			rotateFilterSlot(ms, state, side);
			renderFilterItemStack(filter, ms, collector, light);
			ms.popPose();

			if (active)
				renderFilterOverlay(ms, collector, offset, normal, side, true);
		}
	}

	private static boolean shouldRenderFilterOverlay(BeltTunnelBlockEntity be, BlockState state,
		BrassTunnelFilterSlot slot, Direction side, Vec3 offset) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return false;
		if (!blockHit.getBlockPos()
			.equals(be.getBlockPos()))
			return false;
		if (blockHit.getDirection() != side)
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
		return slot.testHit(be.getLevel(), be.getBlockPos(), state, localHit) || closeEnoughToFilter(side, localHit, offset);
	}

	private static boolean closeEnoughToFilter(Direction side, Vec3 localHit, Vec3 offset) {
		double halfSize = 3 / 16d;
		return switch (side.getAxis()) {
			case X -> Math.abs(localHit.y - offset.y) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
			case Y -> Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
			case Z -> Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.y - offset.y) <= halfSize;
		};
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, Vec3 offset, Vec3 normal,
		Direction side, boolean hasFilter) {
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateFilterSlot(ms, null, side);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	private static void rotateFilterSlot(PoseStack ms, BlockState state, Direction side) {
		rotateToFace(ms, side);
		if (state == null || !side.getAxis()
			.isVertical())
			return;
		if (!state.hasProperty(BeltTunnelBlock.HORIZONTAL_AXIS))
			return;
		float angle = state.getValue(BeltTunnelBlock.HORIZONTAL_AXIS) == Direction.Axis.X ? 90 : 0;
		ms.mulPose(Axis.ZP.rotationDegrees(angle));
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
		switch (face) {
			case SOUTH -> {
			}
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(270));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(270));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
		}
	}

	private static void renderFilterCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderFilterDots(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		flatPixelXY(pose, consumer, 6, 6, color);
		flatPixelXY(pose, consumer, 9, 6, color);
		flatPixelXY(pose, consumer, 6, 9, color);
		flatPixelXY(pose, consumer, 9, 9, color);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y, int xStep, int yStep,
		int color) {
		flatPixelXY(pose, consumer, x, y, color);
		flatPixelXY(pose, consumer, x + xStep, y, color);
		flatPixelXY(pose, consumer, x, y + yStep, color);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		flatPixelXY(pose, consumer, x, y, 0, color);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y, float z, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, z, color);
	}

	private static void flatScaledPixelXY(Pose pose, VertexConsumer consumer, int x, int y, float scale, float z,
		int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, (x * pixel - .5f) * scale, (y * pixel - .5f) * scale,
			((x + 1) * pixel - .5f) * scale, ((y + 1) * pixel - .5f) * scale, z, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		int color) {
		flatQuadXY(pose, consumer, x0, y0, x1, y1, 0, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		float z, int color) {
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y0, z).setColor(color);
	}

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .24f);
	}

	private void renderFlaps(PoseStack ms, SubmitNodeCollector collector, List<BlockStateModelPart> flap,
		Direction side, float flapness, int light) {
		float horizontalAngle = AngleHelper.horizontalAngle(side.getOpposite());

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(horizontalAngle));
		ms.translate(-.5, -.5, -.5);
		ms.translate(FlapStuffs.X_OFFSET, 0, 0);

		for (int segment = 0; segment < FlapStuffs.FLAP_COUNT; segment++) {
			ms.pushPose();
			ms.translate(FlapStuffs.TUNNEL_PIVOT.x, FlapStuffs.TUNNEL_PIVOT.y, FlapStuffs.TUNNEL_PIVOT.z);
			ms.mulPose(Axis.XP.rotationDegrees(FlapStuffs.flapAngle(flapness, segment)));
			ms.translate(-FlapStuffs.TUNNEL_PIVOT.x, -FlapStuffs.TUNNEL_PIVOT.y, -FlapStuffs.TUNNEL_PIVOT.z);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), flap,
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
			ms.translate(FlapStuffs.SEGMENT_STEP, 0, 0);
		}
		ms.popPose();
	}

	private List<BlockStateModelPart> getFlapModel() {
		if (flapModel != null)
			return flapModel;
		BlockStateModelPart flap = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.BELT_TUNNEL_FLAP);
		return flapModel = flap == null ? List.of() : List.of(flap);
	}

	private static class TunnelRenderState extends BlockEntityRenderState {
		BeltTunnelBlockEntity blockEntity;
		float partialTicks;
	}

}
