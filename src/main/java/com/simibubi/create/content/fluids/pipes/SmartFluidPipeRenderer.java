package com.simibubi.create.content.fluids.pipes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SmartFluidPipeRenderer extends SmartBlockEntityRenderer<SmartFluidPipeBlockEntity> {

	private static final int FILTER_OVERLAY_COLOR = 0xFFFFFFFF;
	private static final float PIXEL = 1 / 16f;
	private static final float FILTER_OVERLAY_X_SHIFT_PIXELS = 0;
	private static final float FILTER_OVERLAY_Y_SHIFT_PIXELS = -.5f;
	private static final double FILTER_OVERLAY_NORMAL_SHIFT = .75d / 16d;

	public SmartFluidPipeRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SmartFluidPipeBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SmartFluidPipeRenderState();
	}

	@Override
	public void extractRenderState(SmartFluidPipeBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof SmartFluidPipeRenderState pipeState)
			pipeState.blockEntity = be;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SmartFluidPipeRenderState pipeState))
			return;
		SmartFluidPipeBlockEntity pipe = pipeState.blockEntity;
		if (pipe == null || isInvalid(pipe))
			return;

		renderFilter(pipe, ms, collector, state.lightCoords);
	}

	private static void renderFilter(SmartFluidPipeBlockEntity pipe, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		FilteringBehaviour filtering = pipe.getBehaviour(FilteringBehaviour.TYPE);
		if (filtering == null || !filtering.isActive())
			return;

		BlockState state = pipe.getBlockState();
		SmartFluidPipeBlockEntity.SmartPipeFilterSlot slot = new SmartFluidPipeBlockEntity.SmartPipeFilterSlot();
		if (!slot.shouldRender(pipe.getLevel(), pipe.getBlockPos(), state))
			return;

		Vec3 offset = slot.getLocalOffset(pipe.getLevel(), pipe.getBlockPos(), state);
		if (offset == null)
			return;

		Direction normal = getNormal(offset);
		ItemStack filter = filtering.getFilter();
		if (!filter.isEmpty()) {
			ms.pushPose();
			applyFilterSlotTransform(ms, pipe, state, slot, offset, normal, FILTER_OVERLAY_NORMAL_SHIFT, false);
			renderFilterItemStack(filter, ms, collector, light);
			ms.popPose();
		}

		if (shouldRenderFilterOverlay(pipe, slot))
			renderFilterOverlay(ms, collector, pipe, state, slot, offset, normal, !filter.isEmpty());
	}

	private static boolean shouldRenderFilterOverlay(SmartFluidPipeBlockEntity pipe,
		SmartFluidPipeBlockEntity.SmartPipeFilterSlot slot) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return false;
		if (!blockHit.getBlockPos()
			.equals(pipe.getBlockPos()))
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pipe.getBlockPos()));
		return slot.testHit(pipe.getLevel(), pipe.getBlockPos(), pipe.getBlockState(), localHit);
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, SmartFluidPipeBlockEntity pipe,
		BlockState state, SmartFluidPipeBlockEntity.SmartPipeFilterSlot slot, Vec3 offset, Direction normal,
		boolean hasFilter) {
		ms.pushPose();
		applyFilterSlotTransform(ms, pipe, state, slot, offset, normal, FILTER_OVERLAY_NORMAL_SHIFT, false);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	private static void applyFilterSlotTransform(PoseStack ms, SmartFluidPipeBlockEntity pipe, BlockState state,
		SmartFluidPipeBlockEntity.SmartPipeFilterSlot slot, Vec3 offset, Direction normal, double normalOffset,
		boolean itemScale) {
		Vec3 normalVec = Vec3.atLowerCornerOf(normal.getUnitVec3i());
		ms.translate(offset.x + normalVec.x * normalOffset, offset.y + normalVec.y * normalOffset,
			offset.z + normalVec.z * normalOffset);
		ms.mulPose(Axis.YP.rotationDegrees(slot.angleY(state)));
		ms.mulPose(Axis.XP.rotationDegrees(state.getValue(SmartFluidPipeBlock.FACE) == AttachFace.CEILING ? -45 : 45));
		if (itemScale)
			ms.scale(.5f, .5f, .5f);
	}

	private static Direction getNormal(Vec3 offset) {
		double x = offset.x - .5d;
		double y = offset.y - .5d;
		double z = offset.z - .5d;
		double ax = Math.abs(x);
		double ay = Math.abs(y);
		double az = Math.abs(z);
		if (ay >= ax && ay >= az)
			return y > 0 ? Direction.UP : Direction.DOWN;
		if (ax >= az)
			return x > 0 ? Direction.EAST : Direction.WEST;
		return z > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .24f);
		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(0, 0, 1 / 128f);
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .24f);
		ms.popPose();
	}

	private static void renderFilterCorners(Pose pose, VertexConsumer consumer) {
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1);
	}

	private static void renderFilterDots(Pose pose, VertexConsumer consumer) {
		flatPixelXY(pose, consumer, 6, 6);
		flatPixelXY(pose, consumer, 9, 6);
		flatPixelXY(pose, consumer, 6, 9);
		flatPixelXY(pose, consumer, 9, 9);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y, int xStep, int yStep) {
		flatPixelXY(pose, consumer, x, y);
		flatPixelXY(pose, consumer, x + xStep, y);
		flatPixelXY(pose, consumer, x, y + yStep);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y) {
		float xShifted = x + FILTER_OVERLAY_X_SHIFT_PIXELS;
		float yShifted = y + FILTER_OVERLAY_Y_SHIFT_PIXELS;
		flatQuadXY(pose, consumer, xShifted * PIXEL - .5f, yShifted * PIXEL - .5f, (xShifted + 1) * PIXEL - .5f,
			(yShifted + 1) * PIXEL - .5f);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1) {
		consumer.addVertex(pose, x0, y0, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x1, y0, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x1, y1, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x0, y1, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x0, y1, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x1, y1, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x1, y0, 0)
			.setColor(FILTER_OVERLAY_COLOR);
		consumer.addVertex(pose, x0, y0, 0)
			.setColor(FILTER_OVERLAY_COLOR);
	}

	private static class SmartFluidPipeRenderState extends BlockEntityRenderState {
		private SmartFluidPipeBlockEntity blockEntity;
	}
}
