package com.simibubi.create.content.contraptions.bearing;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BearingRenderer<T extends KineticBlockEntity & IBearingBlockEntity> extends KineticBlockEntityRenderer<T> {
	private List<BlockStateModelPart> shaftHalfModel;
	private List<BlockStateModelPart> bearingTopModel;
	private List<BlockStateModelPart> woodenBearingTopModel;

	public BearingRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof KineticBlockEntity kineticBE))
			return;
		if (!(kineticBE instanceof IBearingBlockEntity bearingBE))
			return;

		T be = (T) kineticBE;
		if (isInvalid(be))
			return;

		renderScrollOptionOverlay(be, ms, collector);
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Direction facing = be.getBlockState()
			.getValue(BlockStateProperties.FACING);
		renderShaftHalf(be, facing.getOpposite(), kineticState.partialTicks, state, ms, collector);
		renderTop(be, bearingBE, facing, kineticState.partialTicks, state, ms, collector);
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	private void renderShaftHalf(T be, Direction shaftSide, float partialTicks, BlockEntityRenderState state,
		PoseStack ms, SubmitNodeCollector collector) {
		List<BlockStateModelPart> shaft = getShaftHalfModel();
		if (shaft.isEmpty())
			return;

		Axis axis = shaftSide.getAxis();
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(axis, getAngleForBe(be, be.getBlockPos(), axis, partialTicks)));
		rotateHalfShaftTo(ms, shaftSide);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaft, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void renderTop(T be, IBearingBlockEntity bearingBE, Direction facing, float partialTicks,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		List<BlockStateModelPart> top = getTopModel(bearingBE.isWoodenTop());
		if (top.isEmpty())
			return;

		float angle = bearingBE.getInterpolatedAngle(partialTicks - 1) / 180f * (float) Math.PI;
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(facing.getAxis(), angle));
		if (facing.getAxis()
			.isHorizontal())
			ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite())));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), top, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void renderScrollOptionOverlay(KineticBlockEntity be, PoseStack ms,
		SubmitNodeCollector collector) {
		ScrollOptionBehaviour<?> scrollOption = getActiveScrollOption(be);
		if (scrollOption == null)
			return;

		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = be.getBlockPos();
		if (!blockHit.getBlockPos()
			.equals(pos))
			return;

		Direction side = blockHit.getDirection();
		BlockState state = be.getBlockState();
		ValueBoxTransform slot = scrollOption.getSlotPositioning();
		Vec3 offset = getActiveSlotOffset(be.getLevel(), pos, state, slot, blockHit, side);
		if (offset == null)
			return;

		AllIcons icon = scrollOption.get()
			.getIcon();
		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateOverlay(ms, state, side);
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
		ms.scale(-1, 1, 1);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), BearingRenderer::renderScrollOptionFrame);

		ms.scale(.25f, .25f, .25f);
		ms.translate(-.5f, -.5f, 1 / 256f);
		collector.submitCustomGeometry(ms, RenderTypes.textSeeThrough(AllIcons.ICON_ATLAS),
			(pose, consumer) -> icon.renderDoubleSided(pose, consumer, 0xDDDDDD));
		ms.popPose();
	}

	private static ScrollOptionBehaviour<?> getActiveScrollOption(KineticBlockEntity be) {
		if (be instanceof WindmillBearingBlockEntity windmill) {
			if (windmill.movementDirection == null || !windmill.movementDirection.isActive())
				return null;
			return windmill.movementDirection;
		}
		if (be instanceof MechanicalBearingBlockEntity mechanical) {
			if (mechanical.movementMode == null || !mechanical.movementMode.isActive())
				return null;
			return mechanical.movementMode;
		}
		if (be instanceof ClockworkBearingBlockEntity clockwork) {
			if (clockwork.operationMode == null || !clockwork.operationMode.isActive())
				return null;
			return clockwork.operationMode;
		}
		return null;
	}

	private static Vec3 getActiveSlotOffset(Level level, BlockPos pos, BlockState state, ValueBoxTransform slot,
		BlockHitResult blockHit, Direction side) {
		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(side);
		if (!slot.shouldRender(level, pos, state))
			return null;
		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pos));
		if (!slot.testHit(level, pos, state, localHit))
			return null;
		return slot.getLocalOffset(level, pos, state);
	}

	private static void rotateOverlay(PoseStack ms, BlockState state, Direction face) {
		rotateToFace(ms, face);
		if (!face.getAxis()
			.isVertical())
			return;
		Direction facing = state.getValue(BlockStateProperties.FACING);
		if (facing.getAxis()
			.isVertical())
			return;
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
		switch (face) {
		case SOUTH -> {
		}
		case NORTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
		case EAST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
		case WEST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270));
		case UP -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270));
		case DOWN -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
		}
	}

	private static void renderScrollOptionFrame(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
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

	private void rotateHalfShaftTo(PoseStack ms, Direction direction) {
		switch (direction) {
		case NORTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
		case EAST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
		case WEST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
		case UP -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
		case DOWN -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
		case SOUTH -> {
		}
		}
	}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		return shaftHalfModel = getModel(CreateStandaloneModels.SHAFT_HALF);
	}

	private List<BlockStateModelPart> getTopModel(boolean wooden) {
		if (wooden) {
			if (woodenBearingTopModel != null)
				return woodenBearingTopModel;
			return woodenBearingTopModel = getModel(CreateStandaloneModels.BEARING_TOP_WOODEN);
		}
		if (bearingTopModel != null)
			return bearingTopModel;
		return bearingTopModel = getModel(CreateStandaloneModels.BEARING_TOP);
	}

	private static List<BlockStateModelPart> getModel(StandaloneModelKey<BlockStateModelPart> key) {
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		return model == null ? List.of() : List.of(model);
	}

}
