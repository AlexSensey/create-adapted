package com.simibubi.create.content.contraptions.actors.roller;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionActorRotation;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class RollerRenderer extends SmartBlockEntityRenderer<RollerBlockEntity> {
	private static final String[] ROLLER_PAVE_ICON = {
		"................",
		"................",
		"................",
		"................",
		"................",
		"......####......",
		".......##.......",
		"................",
		"....########....",
		"....#......#....",
		"....########....",
		"................",
		"................",
		"................",
		"................",
		"................",
	};
	private static final String[] ROLLER_FILL_ICON = {
		"................",
		"................",
		"......####......",
		".......##.......",
		"................",
		"....########....",
		"....#......#....",
		"....########....",
		"................",
		"....##....##....",
		"................",
		"....##....##....",
		"................",
		"....########....",
		"................",
		"................",
	};
	private static final String[] ROLLER_WIDE_FILL_ICON = {
		"................",
		"................",
		"......####......",
		".......##.......",
		"................",
		"....########....",
		"....#......#....",
		"....########....",
		"................",
		"...##......##...",
		"................",
		"..##........##..",
		"................",
		".##############.",
		"................",
		"................",
	};

	private List<BlockStateModelPart> wheelModel;
	private List<BlockStateModelPart> frameModel;

	public RollerRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RollerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// The roller submits its own top overlays in submit(); rendering the generic value-box contents here
		// duplicates the slot graphics and makes them appear to drift from some camera angles.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new RollerRenderState();
	}

	@Override
	public void extractRenderState(RollerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof RollerRenderState rollerState) {
			rollerState.blockEntity = be;
			rollerState.blockState = be.getBlockState();
			rollerState.partialTicks = partialTicks;
			rollerState.speed = be.getAnimatedSpeed();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof RollerRenderState rollerState))
			return;
		RollerBlockEntity be = rollerState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		submitFrame(rollerState.blockState, ms, collector, state.lightCoords);
		submitWheel(rollerState.blockState, rollerState.speed, rollerState.partialTicks, ms, collector,
			state.lightCoords);
		submitFilterItem(be, rollerState.blockState, ms, collector, state.lightCoords);
		if (isHovered(be))
			submitTopOverlays(be, rollerState.blockState, ms, collector, state.lightCoords);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffers) {
		SuperByteBuffer block = CachedBuffers.block(context.state);
		if (!block.isEmpty())
			block.light(LightCoordsUtil.getLightCoords(renderWorld, context.localPos))
				.renderInto(matrices.getModel(), buffers.getBuffer(RenderTypes.cutoutMovingBlock()));
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		submitContraptionFrame(context.state, ms, collector, light);
		Direction facing = context.state.getValue(RollerBlock.FACING);
		float speed = !VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed()
			: -context.getAnimationSpeed();
		if (context.contraption.stalled)
			speed = 0;
		submitContraptionWheel(context.state, ContraptionActorRotation.getAngle(context, speed), ms, collector, light);
	}

	private static void submitContraptionFrame(BlockState blockState, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		List<BlockStateModelPart> frame = getModel(CreateStandaloneModels.ROLLER_FRAME);
		if (frame.isEmpty())
			return;

		Direction facing = blockState.getValue(RollerBlock.FACING);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), frame, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void submitContraptionWheel(BlockState blockState, float angle, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> wheel = getModel(CreateStandaloneModels.ROLLER_WHEEL);
		if (wheel.isEmpty())
			return;

		Direction facing = blockState.getValue(RollerBlock.FACING);
		ms.pushPose();
		ms.translate(0, -.25, 0);
		ms.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
			.scale(17 / 16f));
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.translate(-.5, -.5, -.5);
		ms.mulPose(Axis.XN.rotation(angle));
		ms.translate(0, -.5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), wheel, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void submitFrame(BlockState blockState, PoseStack ms, SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> frame = getFrameModel();
		if (frame.isEmpty())
			return;

		Direction facing = blockState.getValue(RollerBlock.FACING);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), frame, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void submitWheel(BlockState blockState, float speed, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> wheel = getWheelModel();
		if (wheel.isEmpty())
			return;

		Direction facing = blockState.getValue(RollerBlock.FACING);
		ms.pushPose();
		ms.translate(0, -.25, 0);
		ms.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
			.scale(17 / 16f));
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.translate(-.5, -.5, -.5);
		ms.mulPose(Axis.XN.rotation(getWheelAngle(speed, partialTicks)));
		ms.translate(0, -.5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), wheel, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void submitTopOverlays(RollerBlockEntity be, BlockState blockState, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		submitFilterOverlay(be, blockState, ms, collector, light);
		submitModeOverlay(be, blockState, ms, collector);
	}

	private static boolean isHovered(RollerBlockEntity be) {
		HitResult hitResult = Minecraft.getInstance().hitResult;
		return hitResult instanceof BlockHitResult blockHit && blockHit.getBlockPos()
			.equals(be.getBlockPos());
	}

	private static void submitFilterOverlay(RollerBlockEntity be, BlockState blockState, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		ItemStack filter = be.filtering.getFilter();
		Vec3 offset = getValueBoxOffset(blockState, 3);

		ms.pushPose();
		applyTopQuadTransform(ms, blockState, offset);
		ms.scale(.78f, .78f, .78f);
		submitValueBoxMarks(ms, collector, filter.isEmpty());
		ms.popPose();
	}

	private static void submitFilterItem(RollerBlockEntity be, BlockState blockState, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		ItemStack filter = be.filtering.getFilter();
		if (!filter.isEmpty()) {
			ms.pushPose();
			applyTopValueBoxTransform(ms, blockState, getValueBoxOffset(blockState, 3), 1 / 32d + 1 / 256d);
			renderItemIntoValueBox(filter, ms, collector, light);
			ms.popPose();
		}
	}

	private static void submitModeOverlay(RollerBlockEntity be, BlockState blockState, PoseStack ms,
		SubmitNodeCollector collector) {
		Vec3 offset = getValueBoxOffset(blockState, -3);

		ms.pushPose();
		applyTopQuadTransform(ms, blockState, offset);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderRollerModeIcon(pose, consumer, be.mode.getValue()));
		ms.popPose();

		ms.pushPose();
		applyTopQuadTransform(ms, blockState, offset);
		ms.scale(.78f, .78f, .78f);
		submitValueBoxMarks(ms, collector, false);
		ms.popPose();
	}

	private static void submitValueBoxMarks(PoseStack ms, SubmitNodeCollector collector, boolean dots) {
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (dots)
				renderValueBoxDots(pose, consumer);
			else
				renderValueBoxCorners(pose, consumer);
		});
	}

	private static void applyTopValueBoxTransform(PoseStack ms, BlockState blockState, Vec3 offset,
		double normalOffset) {
		applyTopValueBoxTransform(ms, blockState, offset, normalOffset, .5f);
	}

	private static void applyTopValueBoxTransform(PoseStack ms, BlockState blockState, Vec3 offset,
		double normalOffset, float scale) {
		ms.translate(offset.x, offset.y + normalOffset, offset.z);
		Direction facing = blockState.getValue(RollerBlock.FACING);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270));
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
		ms.scale(scale, scale, scale);
	}

	private static void applyTopQuadTransform(PoseStack ms, BlockState blockState, Vec3 offset) {
		ms.translate(offset.x, offset.y + 1 / 32d + 1 / 512d, offset.z);
		ms.mulPose(Axis.XP.rotationDegrees(270));
		Direction facing = blockState.getValue(RollerBlock.FACING);
		ms.mulPose(Axis.ZP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
	}

	private static Vec3 getValueBoxOffset(BlockState blockState, int horizontalOffset) {
		Direction facing = blockState.getValue(RollerBlock.FACING);
		float stateAngle = AngleHelper.horizontalAngle(facing) + 180;
		return VecHelper.rotateCentered(VecHelper.voxelSpace(8 + horizontalOffset, 15.5f, 11), stateAngle,
			Direction.Axis.Y);
	}

	private static void renderItemIntoValueBox(ItemStack stack, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		if (stack.isEmpty())
			return;
		FlatGuiItemRenderer.submit(stack, ms, collector, light, .45f);
	}

	private static void renderValueBoxCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderValueBoxDots(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		flatPixelXY(pose, consumer, 6, 6, color);
		flatPixelXY(pose, consumer, 9, 6, color);
		flatPixelXY(pose, consumer, 6, 9, color);
		flatPixelXY(pose, consumer, 9, 9, color);
	}

	private static void renderRollerModeIcon(Pose pose, VertexConsumer consumer, int mode) {
		int color = 0xFFFFFFFF;
		String[] pixels = switch (mode) {
			case 1 -> ROLLER_FILL_ICON;
			case 2 -> ROLLER_WIDE_FILL_ICON;
			default -> ROLLER_PAVE_ICON;
		};
		for (int y = 0; y < pixels.length; y++) {
			String row = pixels[y];
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) == '#')
					flatScaledPixelXY(pose, consumer, 15 - x, y, .18f, 1 / 128f, color);
			}
		}
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
			(y + 1) * pixel - .5f, 1 / 64f, color);
	}

	private static void flatScaledPixelXY(Pose pose, VertexConsumer consumer, int x, int y, float scale, float z,
		int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, (x * pixel - .5f) * scale, (y * pixel - .5f) * scale,
			((x + 1) * pixel - .5f) * scale, ((y + 1) * pixel - .5f) * scale, z, color);
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

	private List<BlockStateModelPart> getWheelModel() {
		if (wheelModel != null)
			return wheelModel;
		return wheelModel = getModel(CreateStandaloneModels.ROLLER_WHEEL);
	}

	private List<BlockStateModelPart> getFrameModel() {
		if (frameModel != null)
			return frameModel;
		return frameModel = getModel(CreateStandaloneModels.ROLLER_FRAME);
	}

	private static List<BlockStateModelPart> getModel(StandaloneModelKey<BlockStateModelPart> key) {
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		return model == null ? List.of() : List.of(model);
	}

	private static float getWheelAngle(float speed, float partialTicks) {
		if (speed == 0)
			return 0;
		float time = AnimationTickHolder.getRenderTime();
		return ((time * speed / 20f) % 360) / 180 * (float) Math.PI;
	}

	private static class RollerRenderState extends BlockEntityRenderState {
		private RollerBlockEntity blockEntity;
		private BlockState blockState;
		private float partialTicks;
		private float speed;
	}
}
