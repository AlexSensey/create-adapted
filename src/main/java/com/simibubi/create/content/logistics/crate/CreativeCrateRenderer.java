package com.simibubi.create.content.logistics.crate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CreativeCrateRenderer extends SafeBlockEntityRenderer<CreativeCrateBlockEntity> {

	public CreativeCrateRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(CreativeCrateBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new CreativeCrateRenderState();
	}

	@Override
	public void extractRenderState(CreativeCrateBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof CreativeCrateRenderState crateState)
			crateState.blockEntity = be;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof CreativeCrateRenderState crateState))
			return;
		CreativeCrateBlockEntity be = crateState.blockEntity;
		if (be == null || isInvalid(be) || be.filtering == null)
			return;

		renderFilter(be, ms, collector, state.lightCoords);
	}

	private static void renderFilter(CreativeCrateBlockEntity be, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		ItemStack filter = be.filtering.getFilter();
		ValueBoxTransform slot = be.filtering.getSlotPositioning();
		BlockState state = be.getBlockState();
		if (!slot.shouldRender(be.getLevel(), be.getBlockPos(), state))
			return;

		Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), state);
		if (offset == null)
			return;

		boolean active = isFilterHit(be, state, slot);
		if (active)
			renderFilterOverlay(ms, collector, be, state, slot, offset, !filter.isEmpty());

		if (filter.isEmpty())
			return;

		ms.pushPose();
		ms.translate(offset.x, offset.y + 1 / 16d, offset.z);
		rotateToTop(ms);
		ms.scale(.42f, .42f, .42f);
		renderFilterItemStack(filter, ms, collector, light);
		ms.popPose();
	}

	private static boolean isFilterHit(CreativeCrateBlockEntity be, BlockState state, ValueBoxTransform slot) {
		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit) || !blockHit.getBlockPos()
			.equals(be.getBlockPos()))
			return false;
		if (blockHit.getDirection() != Direction.UP)
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
		return slot.testHit(be.getLevel(), be.getBlockPos(), state, localHit);
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, CreativeCrateBlockEntity be,
		BlockState state, ValueBoxTransform slot, Vec3 offset, boolean hasFilter) {
		ms.pushPose();
		ms.translate(offset.x, offset.y + 1 / 32d + 1 / 512d, offset.z);
		rotateToTop(ms);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	private static void rotateToTop(PoseStack ms) {
		ms.mulPose(Axis.XP.rotationDegrees(270));
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
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		int color) {
		consumer.addVertex(pose, x0, y0, 0).setColor(color);
		consumer.addVertex(pose, x1, y0, 0).setColor(color);
		consumer.addVertex(pose, x1, y1, 0).setColor(color);
		consumer.addVertex(pose, x0, y1, 0).setColor(color);
		consumer.addVertex(pose, x0, y1, 0).setColor(color);
		consumer.addVertex(pose, x1, y1, 0).setColor(color);
		consumer.addVertex(pose, x1, y0, 0).setColor(color);
		consumer.addVertex(pose, x0, y0, 0).setColor(color);
	}

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
	}

	private static class CreativeCrateRenderState extends BlockEntityRenderState {
		private CreativeCrateBlockEntity blockEntity;
	}
}
